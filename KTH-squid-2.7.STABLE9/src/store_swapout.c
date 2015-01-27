
/*
 * $Id: store_swapout.c,v 1.96.6.1 2008/05/04 23:23:13 hno Exp $
 *
 * DEBUG: section 20    Storage Manager Swapout Functions
 * AUTHOR: Duane Wessels
 *
 * SQUID Web Proxy Cache          http://www.squid-cache.org/
 * ----------------------------------------------------------
 *
 *  Squid is the result of efforts by numerous individuals from
 *  the Internet community; see the CONTRIBUTORS file for full
 *  details.   Many organizations have provided support for Squid's
 *  development; see the SPONSORS file for full details.  Squid is
 *  Copyrighted (C) 2001 by the Regents of the University of
 *  California; see the COPYRIGHT file for full details.  Squid
 *  incorporates software developed and/or copyrighted by other
 *  sources; see the CREDITS file for full details.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111, USA.
 *
 */

#include "squid.h"

static void storeSwapOutStart(StoreEntry * e);
static STIOCB storeSwapOutFileClosed;
static STIOCB storeSwapOutFileNotify;
static int storeSwapOutAble(const StoreEntry * e);

//Kim Taehee added func start
unsigned char* getStoredFilePrefix(StoreEntry* e);
const cache_key* getMD5Digest(const unsigned char* prefix, StoreEntry* e); //Kim Taehee added
const cache_key * getMatchedSwapoutKey(
		const char* lmt, const int startRange, const int endRange); //Kim Taehee added
void appendChunkToTableFile(char* lmt, int startRange, int endRange,
		char* dataDigest, char* swapoutDigest, StoreEntry* e);
//Kim Taehee added func end

//Kim Taehee added start
YoutubeChunkTable youtubeTable;

//TODO: below is temporary.
//array which is saved data digest info.
//const char* dataDigest[6][2]={ //col 0: datadigest, col 1: matched url digest
//		"B81481242F159F7F4A5886881F0E62E6", "3CF71F0C0A52CFC7C0265D007160C03E", // 0 00000203, 0-65535
//		"B9226AEDECC921063143FB9E39F7B792", "5D835A88FAFD400CB9449F1ADFF61DD6", // 0 00000208, 65536-112336
//		"FE9FD7433E4134F7F704CD36CA900566", "CF5E2345203DF60FE3CA3CEA33E91D8D", // 0 0000020B, 112337-238321
//		"9D879BBE42CC7A5F49EA0A1783DD6C04", "24B70CB4717F7354AE3E88930B4BF868", // 0 0000020D, 238322-492590
//		"CC3460D6932414175F923D630322B690", "2BAEDEA2AF2E0C2FB84D7611B298268F", // 0 0000020F, 492591-1036645
//		"38EDBEF884DDAC40D04CF308ADF60E09", "8F7B288F18A3D9B33AD1C91BEEDBE86E" // 0 00000211, 1036646-1619866
//}; //http://www.youtube.com/watch?v=_SIWDfFR5LU

//TODO: this is fake chunks list to test. ([3] <-> [2])
//const char* dataDigest[4][2]={ //col 0: datadigest, col 1: matched url digest
//		"FC0DBEE9D62D90E6EFA3D93DAA1FE6BC","F28811E75D6B9B225BD1D6999B603D70",
//		"112C6586FF77B268C15D158AD8EE6011","EA81014608BC9366947E45C161FD03BF",
//		"C94B94521443943D80C47E79608BA136","32F712A0B320FE79739D00FF05FD812D",
//		"B33F960B63395C5CBA054FCDA2AEDDBE","43851DF0E763DCABB6DCD4DBB8FACC40",
//};

//int hittingState=0; //1 is hitting, or 0.
//int hittingOffset=0;
//const int numOfchunks=6;
//const char* lmt="1396722499565907"; //http://www.youtube.com/watch?v=_SIWDfFR5LU
// TODO: test lmt "1392948869353355"; http://www.youtube.com/watch?v=FUq1D709JWc
//Kim Taehee added end


/* start swapping object to disk */
static void
storeSwapOutStart(StoreEntry * e)
{
	generic_cbdata *c;
	MemObject *mem = e->mem_obj;
	int swap_hdr_sz = 0;
	tlv *tlv_list;
	char *buf;
	assert(mem);
	/* Build the swap metadata, so the filesystem will know how much
	 * metadata there is to store
	 */
	debug(20, 5) ("storeSwapOutStart: Begin SwapOut '%s' to dirno %d, fileno %08X\n",
			storeUrl(e), e->swap_dirn, e->swap_filen);
	e->swap_status = SWAPOUT_WRITING;
	tlv_list = storeSwapMetaBuild(e);
	buf = storeSwapMetaPack(tlv_list, &swap_hdr_sz);
	storeSwapTLVFree(tlv_list);
	mem->swap_hdr_sz = (size_t) swap_hdr_sz;
	/* Create the swap file */
	c = cbdataAlloc(generic_cbdata);
	c->data = e;
	mem->swapout.sio = storeCreate(e, storeSwapOutFileNotify, storeSwapOutFileClosed, c);
	if (NULL == mem->swapout.sio) {
		e->swap_status = SWAPOUT_NONE;
		cbdataFree(c);
		xfree(buf);
		storeLog(STORE_LOG_SWAPOUTFAIL, e);
		return;
	}
	storeLockObject(e);		/* Don't lock until after create, or the replacement
	 * code might get confused */
	/* Pick up the file number if it was assigned immediately */
	e->swap_filen = mem->swapout.sio->swap_filen;
	e->swap_dirn = mem->swapout.sio->swap_dirn;
	/* write out the swap metadata */
	cbdataLock(mem->swapout.sio);
	storeWrite(mem->swapout.sio, buf, mem->swap_hdr_sz, xfree);
}

static void
storeSwapOutFileNotify(void *data, int errflag, storeIOState * sio)
{
	generic_cbdata *c = data;
	StoreEntry *e = c->data;
	MemObject *mem = e->mem_obj;
	assert(e->swap_status == SWAPOUT_WRITING);
	assert(mem);
	assert(mem->swapout.sio == sio);
	assert(errflag == 0);
	e->swap_filen = mem->swapout.sio->swap_filen;
	e->swap_dirn = mem->swapout.sio->swap_dirn;
}

/* as sideeffect returns if the object can be cached */
int
storeSwapOutMaintainMemObject(StoreEntry * e)
{
	MemObject *mem = e->mem_obj;
	squid_off_t lowest_offset;
	squid_off_t new_mem_lo;
	int swapout_able;

	/* Don't pollute the disk with icons and other special entries */
	if (EBIT_TEST(e->flags, ENTRY_SPECIAL))
		return -1;

	/* Don't even attempt to free data from hot-cached objects */
	if (e->mem_status == IN_MEMORY)
		return 1;

	swapout_able = storeSwapOutAble(e);
	if (!swapout_able) {
		/* Stop writing to disk */
		storeReleaseRequest(e);
		if (e->mem_obj->swapout.sio != NULL)
			storeSwapOutFileClose(e);
	}
	/* storeLowestMemReaderOffset must be after the storeSwapOutFileClose
	 * call above as storeLowestMemReaderOffset needs to know if we are
	 * still writing to disk or not
	 */
	lowest_offset = storeLowestMemReaderOffset(e);
	debug(20, 7) ("storeSwapOutMaintainMemObject: lowest_offset = %" PRINTF_OFF_T "\n",
			lowest_offset);
	if (!swapout_able)
		new_mem_lo = lowest_offset;
	else if (mem->inmem_hi > Config.Store.maxInMemObjSize)
		new_mem_lo = lowest_offset;
	else if (mem->reply->content_length > Config.Store.maxInMemObjSize)
		new_mem_lo = lowest_offset;
	else
		new_mem_lo = mem->inmem_lo;
	/* The -1 makes sure the page isn't freed until storeSwapOut has
	 * walked to the next page. (mem->swapout.memnode->next) */
	if (swapout_able && new_mem_lo) {
		squid_off_t on_disk = storeSwapOutObjectBytesOnDisk(e->mem_obj);
		if (on_disk - 1 < new_mem_lo)
			new_mem_lo = on_disk - 1;
	}
	if (new_mem_lo < mem->inmem_lo)
		new_mem_lo = mem->inmem_lo;
	if (mem->inmem_lo != new_mem_lo) {
		mem->inmem_lo = stmemFreeDataUpto(&mem->data_hdr, new_mem_lo);

		/* If ENTRY_DEFER_READ is set, then the client side will continue to
		 * flush until it has less than READ_AHEAD_GAP bytes in memory */
		if (EBIT_TEST(e->flags, ENTRY_DEFER_READ)) {

			if (mem->inmem_hi - mem->inmem_lo <= Config.readAheadGap) {
				storeResumeRead(e);
			}
		}
	}
	return swapout_able;
}

void
storeSwapOut(StoreEntry * e)
{
	MemObject *mem = e->mem_obj;
	int swapout_able;
	squid_off_t swapout_size;
	size_t swap_buf_len;
	if (mem == NULL)
		return;
	/* should we swap something out to disk? */
	debug(20, 7) ("storeSwapOut: %s\n", storeUrl(e));
	debug(20, 7) ("storeSwapOut: store_status = %s\n",
			storeStatusStr[e->store_status]);
	if (EBIT_TEST(e->flags, ENTRY_ABORTED)) {
		assert(EBIT_TEST(e->flags, RELEASE_REQUEST));
		storeSwapOutFileClose(e);
		return;
	}
	if (EBIT_TEST(e->flags, ENTRY_SPECIAL)) {
		debug(20, 3) ("storeSwapOut: %s SPECIAL\n", storeUrl(e));
		return;
	}
	debug(20, 7) ("storeSwapOut: mem->inmem_lo = %" PRINTF_OFF_T "\n",
			mem->inmem_lo);
	debug(20, 7) ("storeSwapOut: mem->inmem_hi = %" PRINTF_OFF_T "\n",
			mem->inmem_hi);
	debug(20, 7) ("storeSwapOut: swapout.queue_offset = %" PRINTF_OFF_T "\n",
			mem->swapout.queue_offset);
	if (mem->swapout.sio)
		debug(20, 7) ("storeSwapOut: storeOffset() = %" PRINTF_OFF_T "\n",
				storeOffset(mem->swapout.sio));
	assert(mem->inmem_hi >= mem->swapout.queue_offset);
	/*
	 * Grab the swapout_size and check to see whether we're going to defer
	 * the swapout based upon size
	 */
	swapout_size = mem->inmem_hi - mem->swapout.queue_offset;
	if ((e->store_status != STORE_OK) && (swapout_size < store_maxobjsize)) {
		/*
		 * NOTE: the store_maxobjsize here is the max of optional
		 * max-size values from 'cache_dir' lines.  It is not the
		 * same as 'maximum_object_size'.  By default, store_maxobjsize
		 * will be set to -1.  However, I am worried that this
		 * deferance may consume a lot of memory in some cases.
		 * It would be good to make this decision based on reply
		 * content-length, rather than wait to accumulate huge
		 * amounts of object data in memory.
		 */
		debug(20, 5) ("storeSwapOut: Deferring starting swapping out\n");
		return;
	}
	swapout_able = storeSwapOutMaintainMemObject(e);
#if SIZEOF_SQUID_OFF_T <= 4
	if (mem->inmem_hi > 0x7FFF0000) {
		debug(20, 0) ("WARNING: preventing squid_off_t overflow for %s\n", storeUrl(e));
		storeAbort(e);
		return;
	}
#endif
	if (!swapout_able)
		return;
	debug(20, 7) ("storeSwapOut: swapout_size = %" PRINTF_OFF_T "\n",
			swapout_size);
	if (swapout_size == 0) {
		if (e->store_status == STORE_OK)
			storeSwapOutFileClose(e);
		return;			/* Nevermore! */
	}
	if (e->store_status == STORE_PENDING) {
		/* wait for a full block to write */
		if (swapout_size < SM_PAGE_SIZE)
			return;
		/*
		 * Wait until we are below the disk FD limit, only if the
		 * next server-side read won't be deferred.
		 */
		if (storeTooManyDiskFilesOpen() && !fwdCheckDeferRead(-1, e))
			return;
	}
	/* Ok, we have stuff to swap out.  Is there a swapout.sio open? */
	if (e->swap_status == SWAPOUT_NONE && !EBIT_TEST(e->flags, ENTRY_FWD_HDR_WAIT)) {
		assert(mem->swapout.sio == NULL);
		assert(mem->inmem_lo == 0);
		if (storeCheckCachable(e))
			storeSwapOutStart(e);
		else {
			/* Now that we know the data is not cachable, free the memory
			 * to make sure the forwarding code does not defer the connection
			 */
			storeSwapOutMaintainMemObject(e);
			return;
		}
		/* ENTRY_CACHABLE will be cleared and we'll never get here again */
	}
	if (NULL == mem->swapout.sio)
		return;
	do {
		/*
		 * Evil hack time.
		 * We are paging out to disk in page size chunks. however, later on when
		 * we update the queue position, we might not have a page (I *think*),
		 * so we do the actual page update here.
		 */

		if (mem->swapout.memnode == NULL) {
			/* We need to swap out the first page */
			mem->swapout.memnode = mem->data_hdr.head;
		} else {
			/* We need to swap out the next page */
			mem->swapout.memnode = mem->swapout.memnode->next;
		}
		/*
		 * Get the length of this buffer. We are assuming(!) that the buffer
		 * length won't change on this buffer, or things are going to be very
		 * strange. I think that after the copy to a buffer is done, the buffer
		 * size should stay fixed regardless so that this code isn't confused,
		 * but we can look at this at a later date or whenever the code results
		 * in bad swapouts, whichever happens first. :-)
		 */
		swap_buf_len = mem->swapout.memnode->len;

		debug(20, 3) ("storeSwapOut: swap_buf_len = %d\n", (int) swap_buf_len);
		assert(swap_buf_len > 0);
		debug(20, 3) ("storeSwapOut: swapping out %d bytes from %" PRINTF_OFF_T "\n",
				(int) swap_buf_len, mem->swapout.queue_offset);
		mem->swapout.queue_offset += swap_buf_len;
		storeWrite(mem->swapout.sio, stmemNodeGet(mem->swapout.memnode), swap_buf_len, stmemNodeFree);
		/* the storeWrite() call might generate an error */
		if (e->swap_status != SWAPOUT_WRITING)
			break;
		swapout_size = mem->inmem_hi - mem->swapout.queue_offset;
		if (e->store_status == STORE_PENDING)
			if (swapout_size < SM_PAGE_SIZE)
				break;
	} while (swapout_size > 0);
	if (NULL == mem->swapout.sio)
		/* oops, we're not swapping out any more */
		return;
	if (e->store_status == STORE_OK) {
		/*
		 * If the state is STORE_OK, then all data must have been given
		 * to the filesystem at this point because storeSwapOut() is
		 * not going to be called again for this entry.
		 */
		assert(mem->inmem_hi == mem->swapout.queue_offset);
		storeSwapOutFileClose(e);
	}
}

void
storeSwapOutFileClose(StoreEntry * e)
{
	MemObject *mem = e->mem_obj;
	storeIOState *sio = mem->swapout.sio;
	assert(mem != NULL);
	debug(20, 3) ("storeSwapOutFileClose: %s\n", storeKeyText(e->hash.key));
	debug(20, 3) ("storeSwapOutFileClose: sio = %p\n", mem->swapout.sio);
	if (sio == NULL)
		return;
	mem->swapout.sio = NULL;
	storeClose(sio);
}

static void
storeSwapOutFileClosed(void *data, int errflag, storeIOState * sio)
{
	generic_cbdata *c = data;
	StoreEntry *e = c->data;
	MemObject *mem = e->mem_obj;

	//debug(20, 1) ("storeSwapOutFileClosed: function called.\n");
	assert(e->swap_status == SWAPOUT_WRITING);

	cbdataFree(c);
	if (errflag) {
		debug(20, 1) ("storeSwapOutFileClosed: dirno %d, swapfile %08X, errflag=%d\n\t%s\n",
				e->swap_dirn, e->swap_filen, errflag, xstrerror());
		if (errflag == DISK_NO_SPACE_LEFT) {
			storeDirDiskFull(e->swap_dirn);
			storeDirConfigure();
			storeConfigure();
		}
		if (e->swap_filen > 0)
			storeUnlink(e);
		e->swap_filen = -1;
		e->swap_dirn = -1;
		e->swap_status = SWAPOUT_NONE;
		storeReleaseRequest(e);
	} else {
		/* swapping complete */
		debug(20, 3) ("storeSwapOutFileClosed: SwapOut complete: '%s' to %d, %08X\n",
				storeUrl(e), e->swap_dirn, e->swap_filen);
		e->swap_file_sz = objectLen(e) + mem->swap_hdr_sz;
		e->swap_status = SWAPOUT_DONE;
		storeDirUpdateSwapSize(&Config.cacheSwap.swapDirs[e->swap_dirn], e->swap_file_sz, 1);
		if (storeCheckCachable(e)) {
			//Kim Taehee added start
			char* url;
			char* prefix;
			cache_key* dataDigestKey;
			char* lmt;
			int startRange;
			int endRange;
			char* dataDigest;
			char* swapoutDigest;
			//Kim Taehee added end

			storeLog(STORE_LOG_SWAPOUT, e);
			storeDirSwapLog(e, SWAP_LOG_ADD);

			//Kim Taehee added start
			prefix = getStoredFilePrefix(e);
			dataDigestKey = getMD5Digest(prefix, e);
			//debug(20, 1) ("storeSwapOutFileClosed: get body key success\n");

			url = storeUrl(e);

			if(isYouTubeUrl(url)) {
				lmt = getLmtFromUrl(url);
				startRange = getStartRangeFromUrl(url);
				endRange = getEndRangeFromUrl(url);
				dataDigest = storeKeyText(dataDigestKey);
				swapoutDigest = storeKeyText(e->hash.key);

				insertChunkToYoutubeChunkTable(lmt, startRange,
						endRange, dataDigest, swapoutDigest);

				appendChunkToTableFile(lmt, startRange,
						endRange, dataDigest, swapoutDigest, e);

			}

			//Kim Taehee added end
		}
		statCounter.swap.outs++;
	}
	debug(20, 3) ("storeSwapOutFileClosed: %s:%d\n", __FILE__, __LINE__);
	mem->swapout.sio = NULL;
	cbdataUnlock(sio);
	storeSwapOutMaintainMemObject(e);
	storeUnlockObject(e);
}

/**
 * Kim Taehee added
 * search youtube chunks table by lmt.
 * return: lmt matched swapout key. if cannot find, return null.
 */
const cache_key * getMatchedSwapoutKey(
		const char* lmt, const int startRange, const int endRange) {

//	static cache_key digest[SQUID_MD5_DIGEST_LENGTH];
	int i;
	YoutubeChunk* currNode;

	currNode = youtubeTable.head;

	debug(20, 1) ("getMatchedSwapoutKey: called.\n");
	debug(20, 1) ("getMatchedSwapoutKey: youtube table size: %d\n",youtubeTable.size);
	debug(20, 1) ("getMatchedSwapoutKey: youtubeTable.head: %p.\n",youtubeTable.head);

	for(i=0;i<youtubeTable.size;++i) {
		//debug(20, 1) ("getMatchedSwapoutKey: i: %d, currnode addr: %p\n",i,currNode);
		if(currNode) {
			if(strcmp(currNode->lmt,lmt)==0
					&& currNode->startRange == startRange
					&& currNode->endRange == endRange) {
				debug(20, 1) ("getMatchedSwapoutKey: matched on i: %d, swapout key: %s\n",i,currNode->swapoutDigest);
				return storeKeyScan(currNode->swapoutDigest);
			}
		}

		currNode = currNode->next;
	}
	debug(20, 1) ("getMatchedSwapoutKey: no matched swapout key\n");

	return NULL;
}

/**
 * Kim Taehee added.
 * desc: this call will be invoked when not duplicated chunk is exist on table.(TCP_MISS)
 */
void insertChunkToYoutubeChunkTable(char* lmt, int startRange,
		int endRange, char* dataDigest, char* swapoutDigest) {

	int i;
	YoutubeChunk* newNode = (YoutubeChunk*)xmalloc(sizeof(YoutubeChunk));
	YoutubeChunk* currNode = youtubeTable.head;

	debug(20, 1) ("insertChunkToYoutubeChunkTable: called\n");
	debug(20, 1) ("insertChunkToYoutubeChunkTable: newnode addr: %p\n",newNode);
	debug(20, 1) ("getMatchedSwapoutKey: youtubeTable.head: %p.\n",youtubeTable.head);

	//debug(20, 1) ("insertChunkToYoutubeChunkTable: lmt: %s\n",lmt);
	strcpy(newNode->lmt,lmt);
	//debug(20, 1) ("insertChunkToYoutubeChunkTable: newNode->lmt: %s\n",newNode->lmt);
	newNode->startRange = startRange;
	newNode->endRange = endRange;
	//debug(20, 1) ("insertChunkToYoutubeChunkTable: dataDigest: %s\n",dataDigest);
	strcpy(newNode->dataDigest,dataDigest);
	//debug(20, 1) ("insertChunkToYoutubeChunkTable: newNode->dataDigest: %s\n",newNode->dataDigest);
	//debug(20, 1) ("insertChunkToYoutubeChunkTable: swapoutDigest: %s\n",swapoutDigest);
	strcpy(newNode->swapoutDigest,swapoutDigest);
	//debug(20, 1) ("insertChunkToYoutubeChunkTable: newNode->swapoutDigest: %s\n",newNode->swapoutDigest);
	newNode->next = NULL;

	for(i=0;i<youtubeTable.size-1;++i) {
		currNode = currNode->next;
		//debug(20, 1) ("insertChunkToYoutubeChunkTable: i: %d, currnode addr: %p\n",i,currNode);
	}

	//append on end of table
	if(youtubeTable.size == 0) {
		youtubeTable.head = newNode;
	} else {
		currNode->next = newNode;
	}
	youtubeTable.size++;

	debug(20, 1) ("insertChunkToYoutubeChunkTable: table size: %d\n",youtubeTable.size);
	//TODO: release mem when app finish
}

/**
 * Kim Taehee added.
 * desc: write chunk to table file()
 */
void appendChunkToTableFile(char* lmt, int startRange, int endRange,
		char* dataDigest, char* swapoutDigest, StoreEntry* e) {

	SwapDir *sd;
	int fd;
	char tableFilePath[256];
	sd = &Config.cacheSwap.swapDirs[e->swap_dirn]; //..../var/cache

	strcpy(tableFilePath, sd->path);
	strcat(tableFilePath, "/youtubetable.state");

	debug(20, 1) ("appendChunkToTableFile: called. tableFilePath: %s\n",tableFilePath);

	fd = file_open(tableFilePath,O_WRONLY|O_CREAT|O_APPEND);

	if(fd<0) {
		debug(20, 1) ("appendChunkToTableFile: FATAL: cannot open file %s\n",tableFilePath);
		exit(1);
	}

	dprintf(fd,"%s,%d,%d,%s,%s@\n",lmt,startRange,endRange,dataDigest,swapoutDigest);


}

/**
 * Kim Taehee added. get file prefix string(ex: 4096B) to make hash digest.
 */
unsigned char* getStoredFilePrefix(StoreEntry* e) {
	SwapDir *sd; //Kim Taehee added
	char* fullpath;
	int fd;
	//int hdr_sz = e->mem_obj->swap_hdr_sz; //   reply->hdr_sz;
	const int prefixSize = 4096; //byte to read
	unsigned char* prefix;

	char buf[8192];
	int unit=8192;
	char* pos; //offset pointer
	int n;
	int readBytes=0; //read bytes
	int headerSize;
	int i;
	int seekResult;
	int readResult;

	char logTemp[9000];
	int sprintfOffset;

	prefix = (unsigned char*)xcalloc(prefixSize,sizeof(unsigned char));
	sd = &Config.cacheSwap.swapDirs[e->swap_dirn];
	//debug(20, 1) ("swapDir: %s\nswap_dirn: %d\nswap_filen: %08X\n", sd->path,e->swap_dirn,e->swap_filen);

	fullpath = storeUfsDirFullPath(sd,e->swap_filen,NULL);
	debug(20, 1) ("getStoredFilePrefix: fullpath: %s\n",fullpath);

	fd = file_open(fullpath,O_RDONLY|O_BINARY);
	if(fd<0) {
		debug(20, 1) ("getStoredFilePrefix: FATAL: cannot open file %s\n",fullpath);
		exit(1);
	}

	//get body offset
	while((n=read(fd, buf, unit)) >= 0) { //read file loop
		//printf("%s\n",buf);
		char needle[4] = {0x0d, 0x0a, 0x0d, 0x0a};	//\r\n\r\n

		pos = memmem(buf, n, needle, 4);

		if(pos!=NULL) {
			//printf("found at pos: %p, break.\n",pos);
			break;
		}

		readBytes+=n;
		//printf("read in loop: %d\n",read);
	}

	if(pos!=NULL) {
		//printf("read: %d, pos: %p, buf: %p\n",read, pos, buf);
		headerSize = readBytes + (pos - buf) + 4;
	}
	debug(20, 1) ("getStoredFilePrefix: header size: %d\n",headerSize);

	seekResult = lseek(fd,headerSize,SEEK_SET);
	debug(20, 1) ("getStoredFilePrefix: lseek result: %d\n",seekResult);
	if(seekResult != headerSize) {
		debug(20, 1) ("getStoredFilePrefix: FATAL: lseek err\n"); //TODO: fatal err
		exit(1);
	}
	if((readResult = read(fd, prefix, prefixSize)) < 0) {
		debug(20, 1) ("getStoredFilePrefix: FATAL: read err\n"); //TODO: fatal err
		exit(1);
	}
	debug(20, 1) ("getStoredFilePrefix: read result: %d\n",readResult);

	file_close(fd);

	//manual log
//	sprintfOffset=0;
//	for(i=0;i<4096;++i) {
//		sprintfOffset += snprintf(logTemp + sprintfOffset,2,"%02X",prefix[i]);
//		debug(20, 1) ("sprintfOffset: %d, prefix: %02X\n",sprintfOffset,prefix[i]);
//	}
//	debug(20, 1) ("getStoredFilePrefix: prefix is %s\n",logTemp);

	return prefix;
}

/**
 * Kim Taehee added. get MD5 hash digest from prefix string.
 */
const cache_key* getMD5Digest(const unsigned char* prefix, StoreEntry* e) {
	static cache_key digest[SQUID_MD5_DIGEST_LENGTH];
	//char* testStr; //TODO: test
	SQUID_MD5_CTX M;
	int replyBodySize = httpReplyBodySize(e->mem_obj->method, e->mem_obj->reply);
	debug(20, 1) ("getMD5Digest: replyBodySize: %d\n",replyBodySize);

	SQUID_MD5Init(&M);
	SQUID_MD5Update(&M, (unsigned char *) prefix, 4096); //origin is unsigned char
	SQUID_MD5Update(&M, &replyBodySize, sizeof(replyBodySize));

	SQUID_MD5Final(digest, &M);
	debug(20, 1) ("getMD5Digest: digest: %s\n",storeKeyText(digest));

	//TODO: test
//	testStr = storeKeyText(digest);
//	debug(20, 1) ("getMD5Digest: digest: %s\n",
			//storeKeyText(storeKeyScan(testStr)));

	return digest;
}


/*
 * How much of the object data is on the disk?
 */
squid_off_t
storeSwapOutObjectBytesOnDisk(const MemObject * mem)
{
	/*
	 * NOTE: storeOffset() represents the disk file size,
	 * not the amount of object data on disk.
	 *
	 * If we don't have at least 'swap_hdr_sz' bytes
	 * then none of the object data is on disk.
	 *
	 * This should still be safe if swap_hdr_sz == 0,
	 * meaning we haven't even opened the swapout file
	 * yet.
	 */
	off_t nwritten;
	if (mem->swapout.sio == NULL)
		return mem->swapout.queue_offset;
	nwritten = storeOffset(mem->swapout.sio);
	if (nwritten <= mem->swap_hdr_sz)
		return 0;
	return nwritten - mem->swap_hdr_sz;
}

/*
 * Is this entry a candidate for writing to disk?
 */
static int
storeSwapOutAble(const StoreEntry * e)
{
	if (e->mem_obj->inmem_hi > Config.Store.maxObjectSize)
		return 0;
	if (!EBIT_TEST(e->flags, ENTRY_CACHABLE))
		return 0;
	if (e->mem_obj->swapout.sio != NULL)
		return 1;
	if (e->mem_obj->swapout.queue_offset)
		if (e->mem_obj->swapout.queue_offset == e->mem_obj->inmem_hi)
			return 1;
	if (e->mem_obj->inmem_lo > 0)
		return 0;
	return 1;
}
