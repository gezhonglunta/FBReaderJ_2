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

#ifndef __ZLTEXTMODEL_H__
#define __ZLTEXTMODEL_H__

#include <jni.h>

#include <vector>
#include <string>
#include <algorithm>

#include <ZLHyperlinkType.h>
#include <ZLTextParagraph.h>
#include <ZLTextKind.h>
//#include <ZLTextMark.h>
#include <ZLCachedMemoryAllocator.h>

class ZLTextStyleEntry;

class ZLTextModel {

protected:
	ZLTextModel(const std::string &id, const std::string &language, const size_t rowSize,
		const std::string &directoryName, const std::string &fileExtension);
	ZLTextModel(const std::string &id, const std::string &language,
		shared_ptr<ZLCachedMemoryAllocator> allocator);

public:
	virtual ~ZLTextModel();

	const std::string &id() const;
	const std::string &language() const;
	//bool isRtl() const;

	size_t paragraphsNumber() const;
	ZLTextParagraph *operator [] (size_t index);
	const ZLTextParagraph *operator [] (size_t index) const;
/*
	const std::vector<ZLTextMark> &marks() const;

	virtual void search(const std::string &text, size_t startIndex, size_t endIndex, bool ignoreCase) const;
	virtual void selectParagraph(size_t index) const;
	void removeAllMarks();

	ZLTextMark firstMark() const;
	ZLTextMark lastMark() const;
	ZLTextMark nextMark(ZLTextMark position) const;
	ZLTextMark previousMark(ZLTextMark position) const;
*/
	void addControl(ZLTextKind textKind, bool isStart);
	void addStyleEntry(const ZLTextStyleEntry &entry);
	void addStyleCloseEntry();
	void addHyperlinkControl(ZLTextKind textKind, ZLHyperlinkType hyperlinkType, const std::string &label);
	void addText(const std::string &text);
	void addText(const std::vector<std::string> &text);
	void addImage(const std::string &id, short vOffset, bool isCover);
	void addFixedHSpace(unsigned char length);
	void addBidiReset();

	void flush();

	const ZLCachedMemoryAllocator &allocator() const;

	const std::vector<jint> &startEntryIndices() const;
	const std::vector<jint> &startEntryOffsets() const;
	const std::vector<jint> &paragraphLengths() const;
	const std::vector<jint> &textSizes() const;
	const std::vector<jbyte> &paragraphKinds() const;

protected:
	void addParagraphInternal(ZLTextParagraph *paragraph);

private:
	const std::string myId;
	const std::string myLanguage;
	std::vector<ZLTextParagraph*> myParagraphs;
	//mutable std::vector<ZLTextMark> myMarks;
	mutable shared_ptr<ZLCachedMemoryAllocator> myAllocator;

	char *myLastEntryStart;

	std::vector<jint> myStartEntryIndices;
	std::vector<jint> myStartEntryOffsets;
	std::vector<jint> myParagraphLengths;
	std::vector<jint> myTextSizes;
	std::vector<jbyte> myParagraphKinds;

private:
	ZLTextModel(const ZLTextModel&);
	const ZLTextModel &operator = (const ZLTextModel&);
};

class ZLTextPlainModel : public ZLTextModel {

public:
	ZLTextPlainModel(const std::string &id, const std::string &language, const size_t rowSize,
			const std::string &directoryName, const std::string &fileExtension);
	ZLTextPlainModel(const std::string &id, const std::string &language,
		shared_ptr<ZLCachedMemoryAllocator> allocator);
	void createParagraph(ZLTextParagraph::Kind kind);
};

inline const std::string &ZLTextModel::id() const { return myId; }
inline const std::string &ZLTextModel::language() const { return myLanguage; }
inline size_t ZLTextModel::paragraphsNumber() const { return myParagraphs.size(); }
//inline const std::vector<ZLTextMark> &ZLTextModel::marks() const { return myMarks; }
//inline void ZLTextModel::removeAllMarks() { myMarks.clear(); }
inline const ZLCachedMemoryAllocator &ZLTextModel::allocator() const { return *myAllocator; }
inline const std::vector<jint> &ZLTextModel::startEntryIndices() const { return myStartEntryIndices; }
inline const std::vector<jint> &ZLTextModel::startEntryOffsets() const { return myStartEntryOffsets; }
inline const std::vector<jint> &ZLTextModel::paragraphLengths() const { return myParagraphLengths; };
inline const std::vector<jint> &ZLTextModel::textSizes() const { return myTextSizes; };
inline const std::vector<jbyte> &ZLTextModel::paragraphKinds() const { return myParagraphKinds; };

inline ZLTextParagraph *ZLTextModel::operator [] (size_t index) {
	return myParagraphs[std::min(myParagraphs.size() - 1, index)];
}

inline const ZLTextParagraph *ZLTextModel::operator [] (size_t index) const {
	return myParagraphs[std::min(myParagraphs.size() - 1, index)];
}

#endif /* __ZLTEXTMODEL_H__ */
