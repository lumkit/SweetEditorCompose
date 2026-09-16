#ifndef SWEETLINE_FOUNDATION_H
#define SWEETLINE_FOUNDATION_H

#ifdef SWEETLINE_DEBUG
#include <nlohmann/json.hpp>
#include <iostream>
#endif

#include <cstdint>
#include "sweetline/macro.h"

namespace NS_SWEETLINE {
  /// Text position descriptor
  struct TextPosition {
    /// Line number (0-based)
    size_t line {0};
    /// Column number (0-based)
    size_t column {0};
    /// Character index in the full text (0-based)
    size_t index {0};

    bool operator<(const TextPosition& other) const;
    bool operator==(const TextPosition& other) const;
#ifdef SWEETLINE_DEBUG
    void dump() const;
#endif
  };

  /// Text range descriptor
  struct TextRange {
    TextPosition start;
    TextPosition end;

    bool operator==(const TextRange& other) const;
    bool contains(const TextPosition& pos) const;
#ifdef SWEETLINE_DEBUG
    void dump() const;
#endif
  };

  /// Line ending type
  enum struct LineEnding {
    NONE, // Last line without line ending
    LF, // \n
    CRLF, // \r\n
    CR // \r
  };

  /// Document line structure
  struct DocumentLine {
    /// Text content of the current line (excluding line ending)
    U8String text;
    /// Line ending type of the current line
    LineEnding ending {LineEnding::NONE};
  };

  /// Patch result
  struct PatchResult {
    /// Total line count delta after the patch
    int32_t line_delta {0};
    /// Total character count delta after the patch
    int32_t char_delta {0};
  };

  /// Text document with incremental update support
  class Document {
  public:
    explicit Document(const U8String& uri, const U8String& initial_text = "");
    explicit Document(U8String&& uri, const U8String& initial_text = "");

    /// Set the full text content, which will be split into lines
    /// @param text Text content
    void setText(const U8String& text);

    /// Get the URI of the current document
    U8String getUri() const;

    /// Get the full text content
    U8String getText() const;

    /// Get total character count
    size_t totalChars() const;

    /// Get the total character count of a specific line
    /// @param line Line index
    size_t getLineCharCount(size_t line) const;

    /// Get total line count
    size_t getLineCount() const;

    /// Get the text information of a specific line
    /// @param line Line index
    const DocumentLine& getLine(size_t line) const;

    /// Get the text content of a specific line (including line ending)
    /// @param line Line index
    U8String getLineTextWithEnding(size_t line) const;

    /// Perform an incremental update on the specified line/column range
    /// @param range The range to update
    /// @param new_text The replacement text
    /// @return Total line and character deltas after the patch
    PatchResult patch(const TextRange& range, const U8String& new_text);

    /// Append text
    /// @param text Text to append
    PatchResult appendText(const U8String& text);

    /// Insert text at the specified position
    /// @param position Insert position
    /// @param text Text to insert
    void insert(const TextPosition& position, const U8String& text);

    /// Remove text in the specified range
    /// @param range The range to remove
    void remove(const TextRange& range);

    /// Calculate the starting character index of a specific line in the full text
    /// @param line Line number
    size_t charIndexOfLine(size_t line) const;

    /// Convert a character index to a line/column position
    /// @param char_index Character index
    /// @return Line/column position
    TextPosition charIndexToPosition(size_t char_index) const;

    /// Get the width of the line ending
    /// @param ending Line ending type
    /// @return Width of the line ending
    static uint8_t getLineEndingWidth(LineEnding ending);
  private:
    friend class TextAnalyzer;
    U8String m_uri_;
    List<DocumentLine> m_lines_;
    List<size_t> m_line_total_widths_;
    List<size_t> m_line_start_indices_;
    bool isValidPosition(const TextPosition& pos) const;
    size_t positionToCharIndex(const TextPosition& pos) const;
    void rebuildLineMetrics();
    void rebuildLineMetricsFrom(size_t start_line);
    static size_t getLineTotalWidth(const DocumentLine& line);

    static void splitTextIntoLines(const U8String& text, List<DocumentLine>& result);
    PatchResult patchSingleLine(const TextRange& range, const List<DocumentLine>& new_lines);
    PatchResult patchMultipleLines(const TextRange& range, const List<DocumentLine>& new_lines);
    static void appendLineEnding(U8String& text, LineEnding ending);
  };
}

#endif //SWEETLINE_FOUNDATION_H
