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

#include <ZLFile.h>
#include <ZLInputStream.h>

#include "RtfPlugin.h"
#include "RtfDescriptionReader.h"
#include "RtfBookReader.h"
#include "RtfReaderStream.h"

#include "../../bookmodel/BookModel.h"
#include "../../library/Book.h"

bool RtfPlugin::providesMetaInfo() const {
	return false;
}

const std::string RtfPlugin::supportedFileType() const {
	return "rtf";
}

bool RtfPlugin::readMetaInfo(Book &book) const {
	if (!RtfDescriptionReader(book).readDocument(book.file())) {
		return false;
	}

	if (book.encoding().empty()) {
		book.setEncoding("utf-8");
	} else if (book.language().empty()) {
		shared_ptr<ZLInputStream> stream = new RtfReaderStream(book.file(), 50000);
		if (!stream.isNull()) {
			detectLanguage(book, *stream);
		}
	}

	return true;
}

bool RtfPlugin::readModel(BookModel &model) const {
	const Book &book = *model.book();
	return RtfBookReader(model, book.encoding()).readDocument(book.file());
}

bool RtfPlugin::readLanguageAndEncoding(Book &book) const {
	return true;
}
