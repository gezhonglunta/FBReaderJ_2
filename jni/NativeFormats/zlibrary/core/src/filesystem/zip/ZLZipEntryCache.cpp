/*
 * Copyright (C) 2004-2012 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

#include <AndroidUtil.h>
#include <ZLLogger.h>

#include "ZLZip.h"
#include "ZLZipHeader.h"

const size_t ZLZipEntryCache::ourStorageSize = 5;
shared_ptr<ZLZipEntryCache> *ZLZipEntryCache::ourStoredCaches =
	 new shared_ptr<ZLZipEntryCache>[ourStorageSize];
size_t ZLZipEntryCache::ourIndex = 0;

shared_ptr<ZLZipEntryCache> ZLZipEntryCache::cache(const std::string &containerName, ZLInputStream &containerStream) {
	//ZLLogger::Instance().registerClass("ZipEntryCache");
	//ZLLogger::Instance().println("ZipEntryCache", "requesting cache for " + containerName);
	for (size_t i = 0; i < ourStorageSize; ++i) {
		shared_ptr<ZLZipEntryCache> cache = ourStoredCaches[i];
		if (!cache.isNull() && cache->myContainerName == containerName) {
			return cache;
		}
	}
	shared_ptr<ZLZipEntryCache> cache = new ZLZipEntryCache(containerName, containerStream);
	ourStoredCaches[ourIndex] = cache;
	ourIndex = (ourIndex + 1) % ourStorageSize;
	return cache;
}

ZLZipEntryCache::Info::Info() : Offset(-1) {
}

ZLZipEntryCache::ZLZipEntryCache(const std::string &containerName, ZLInputStream &containerStream) : myContainerName(containerName) {
	//ZLLogger::Instance().println("ZipEntryCache", "creating cache for " + containerName);
	if (!containerStream.open()) {
		return;
	}

	ZLZipHeader header;
	while (header.readFrom(containerStream)) {
		Info *infoPtr = 0;
		if (header.Signature == (unsigned long)ZLZipHeader::SignatureLocalFile) {
			std::string entryName(header.NameLength, '\0');
			if ((unsigned int)containerStream.read((char*)entryName.data(), header.NameLength) == header.NameLength) {
				entryName = AndroidUtil::convertNonUtfString(entryName);
				Info &info = myInfoMap[entryName];
				info.Offset = containerStream.offset() + header.ExtraLength;
				info.CompressionMethod = header.CompressionMethod;
				info.CompressedSize = header.CompressedSize;
				info.UncompressedSize = header.UncompressedSize;
				infoPtr = &info;
			}
		}
		ZLZipHeader::skipEntry(containerStream, header);
		if (infoPtr != 0) {
			infoPtr->UncompressedSize = header.UncompressedSize;
		}
	}
	containerStream.close();
}

ZLZipEntryCache::Info ZLZipEntryCache::info(const std::string &entryName) const {
	std::map<std::string,Info>::const_iterator it = myInfoMap.find(entryName);
	return (it != myInfoMap.end()) ? it->second : Info();
}

void ZLZipEntryCache::collectFileNames(std::vector<std::string> &names) const {
	for (std::map<std::string,Info>::const_iterator it = myInfoMap.begin(); it != myInfoMap.end(); ++it) {
		names.push_back(it->first);
	}
}
