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
#include <JniEnvelope.h>

#include <ZLImage.h>
#include <ZLFileImage.h>
#include <ZLLogger.h>
#include <ZLCachedMemoryAllocator.h>
#include <ZLTextStyleEntry.h>

#include "BookReader.h"
#include "BookModel.h"

#include "../library/Book.h"
#include "../library/Library.h"

BookReader::BookReader(BookModel &model) : myModel(model) {
	myCurrentTextModel = 0;

	myTextParagraphExists = false;

	myInsideTitle = false;
	mySectionContainsRegularContents = false;
}

BookReader::~BookReader() {
}

void BookReader::setMainTextModel() {
	myCurrentTextModel = myModel.myBookTextModel;
}

void BookReader::setFootnoteTextModel(const std::string &id) {
	std::map<std::string,shared_ptr<ZLTextModel> >::iterator it = myModel.myFootnotes.find(id);
	if (it != myModel.myFootnotes.end()) {
		myCurrentTextModel = (*it).second;
	} else {
		if (myFootnotesAllocator.isNull()) {
			myFootnotesAllocator = new ZLCachedMemoryAllocator(8192, Library::Instance().cacheDirectory(), "footnotes");
		}
		myCurrentTextModel = new ZLTextPlainModel(id, myModel.myBookTextModel->language(), myFootnotesAllocator);
		myModel.myFootnotes.insert(std::make_pair(id, myCurrentTextModel));
	}
}

void BookReader::unsetTextModel() {
	myCurrentTextModel = 0;
}

void BookReader::pushKind(FBTextKind kind) {
	myKindStack.push_back(kind);
}

bool BookReader::popKind() {
	if (!myKindStack.empty()) {
		myKindStack.pop_back();
		return true;
	}
	return false;
}

bool BookReader::isKindStackEmpty() const {
	return myKindStack.empty();
}

void BookReader::beginParagraph(ZLTextParagraph::Kind kind) {
	endParagraph();
	if (myCurrentTextModel != 0) {
		((ZLTextPlainModel&)*myCurrentTextModel).createParagraph(kind);
		for (std::vector<FBTextKind>::const_iterator it = myKindStack.begin(); it != myKindStack.end(); ++it) {
			myCurrentTextModel->addControl(*it, true);
		}
		if (!myHyperlinkReference.empty()) {
			myCurrentTextModel->addHyperlinkControl(myHyperlinkKind, myHyperlinkType, myHyperlinkReference);
		}
		myTextParagraphExists = true;
	}
}

void BookReader::endParagraph() {
	if (myTextParagraphExists) {
		flushTextBufferToParagraph();
		myTextParagraphExists = false;
	}
}

void BookReader::addControl(FBTextKind kind, bool start) {
	if (myTextParagraphExists) {
		flushTextBufferToParagraph();
		myCurrentTextModel->addControl(kind, start);
	}
	if (!start && !myHyperlinkReference.empty() && (kind == myHyperlinkKind)) {
		myHyperlinkReference.erase();
	}
}

void BookReader::addStyleEntry(const ZLTextStyleEntry &entry) {
	if (myTextParagraphExists) {
		flushTextBufferToParagraph();
		myCurrentTextModel->addStyleEntry(entry);
	}
}

void BookReader::addStyleCloseEntry() {
	if (myTextParagraphExists) {
		flushTextBufferToParagraph();
		myCurrentTextModel->addStyleCloseEntry();
	}
}

void BookReader::addFixedHSpace(unsigned char length) {
	if (myTextParagraphExists) {
		myCurrentTextModel->addFixedHSpace(length);
	}
}

void BookReader::addHyperlinkControl(FBTextKind kind, const std::string &label) {
	myHyperlinkKind = kind;
	std::string type;
	switch (myHyperlinkKind) {
		case INTERNAL_HYPERLINK:
		case FOOTNOTE:
			myHyperlinkType = HYPERLINK_INTERNAL;
			type = "internal";
			break;
		case EXTERNAL_HYPERLINK:
			myHyperlinkType = HYPERLINK_EXTERNAL;
			type = "external";
			break;
		/*case BOOK_HYPERLINK:
			myHyperlinkType = HYPERLINK_BOOK;
			type = "book";
			break;*/
		default:
			myHyperlinkType = HYPERLINK_NONE;
			break;
	}
	ZLLogger::Instance().println(
		"hyperlink",
		" + control (" + type + "): " + label
	);
	if (myTextParagraphExists) {
		flushTextBufferToParagraph();
		myCurrentTextModel->addHyperlinkControl(kind, myHyperlinkType, label);
	}
	myHyperlinkReference = label;
}

void BookReader::addHyperlinkLabel(const std::string &label) {
	if (!myCurrentTextModel.isNull()) {
		int paragraphNumber = myCurrentTextModel->paragraphsNumber();
		if (myTextParagraphExists) {
			--paragraphNumber;
		}
		addHyperlinkLabel(label, paragraphNumber);
	}
}

void BookReader::addHyperlinkLabel(const std::string &label, int paragraphNumber) {
	ZLLogger::Instance().println(
		"hyperlink",
		" + label: " + label
	);
	myModel.myInternalHyperlinks.insert(std::make_pair(
		label, BookModel::Label(myCurrentTextModel, paragraphNumber)
	));
}

void BookReader::addData(const std::string &data) {
	if (!data.empty() && myTextParagraphExists) {
		if (!myInsideTitle) {
			mySectionContainsRegularContents = true;
		}
		myBuffer.push_back(data);
	}
}

void BookReader::addContentsData(const std::string &data) {
	if (!data.empty() && !myContentsTreeStack.empty()) {
		myContentsTreeStack.top()->addText(data);
	}
}

void BookReader::flushTextBufferToParagraph() {
	myCurrentTextModel->addText(myBuffer);
	myBuffer.clear();
}

void BookReader::addImage(const std::string &id, shared_ptr<const ZLImage> image) {
	if (image.isNull()) {
		return;
	}

	JNIEnv *env = AndroidUtil::getEnv();

	jobject javaImage = AndroidUtil::createJavaImage(env, (const ZLFileImage&)*image);
	jstring javaId = AndroidUtil::createJavaString(env, id);
	AndroidUtil::Method_NativeBookModel_addImage->call(myModel.myJavaModel, javaId, javaImage);

	env->DeleteLocalRef(javaId);
	env->DeleteLocalRef(javaImage);
}

void BookReader::insertEndParagraph(ZLTextParagraph::Kind kind) {
	if ((myCurrentTextModel != 0) && mySectionContainsRegularContents) {
		size_t size = myCurrentTextModel->paragraphsNumber();
		if ((size > 0) && (((*myCurrentTextModel)[(size_t)-1])->kind() != kind)) {
			((ZLTextPlainModel&)*myCurrentTextModel).createParagraph(kind);
			mySectionContainsRegularContents = false;
		}
	}
}

void BookReader::insertEndOfSectionParagraph() {
	insertEndParagraph(ZLTextParagraph::END_OF_SECTION_PARAGRAPH);
}

void BookReader::insertEndOfTextParagraph() {
	insertEndParagraph(ZLTextParagraph::END_OF_TEXT_PARAGRAPH);
}

void BookReader::addImageReference(const std::string &id, short vOffset, bool isCover) {
	if (myCurrentTextModel != 0) {
		mySectionContainsRegularContents = true;
		if (myTextParagraphExists) {
			flushTextBufferToParagraph();
			myCurrentTextModel->addImage(id, vOffset, isCover);
		} else {
			beginParagraph();
			myCurrentTextModel->addControl(IMAGE, true);
			myCurrentTextModel->addImage(id, vOffset, isCover);
			myCurrentTextModel->addControl(IMAGE, false);
			endParagraph();
		}
	}
}

void BookReader::beginContentsParagraph(int referenceNumber) {
	if (myCurrentTextModel == myModel.myBookTextModel) {
		if (referenceNumber == -1) {
			referenceNumber = myCurrentTextModel->paragraphsNumber();
		}
		shared_ptr<ContentsTree> parent =
			myContentsTreeStack.empty() ? myModel.contentsTree() : myContentsTreeStack.top();
		if (parent->text().empty()) {
			parent->addText("...");
		}
		new ContentsTree(*parent, referenceNumber);
		const std::vector<shared_ptr<ContentsTree> > &children = parent->children();
		myContentsTreeStack.push(children[children.size() - 1]);
		myContentsParagraphExists = true;
	}
}

void BookReader::endContentsParagraph() {
	if (!myContentsTreeStack.empty()) {
		shared_ptr<ContentsTree> tree = myContentsTreeStack.top();
		if (tree->text().empty()) {
			tree->addText("...");
		}
		myContentsTreeStack.pop();
	}
	myContentsParagraphExists = false;
}

/*
void BookReader::setReference(size_t contentsParagraphNumber, int referenceNumber) {
	ContentsModel &contentsModel = (ContentsModel&)*myModel.myContentsModel;
	if (contentsParagraphNumber >= contentsModel.paragraphsNumber()) {
		return;
	}
	contentsModel.setReference((const ZLTextTreeParagraph*)contentsModel[contentsParagraphNumber], referenceNumber);
}
*/

void BookReader::reset() {
	myKindStack.clear();
}
