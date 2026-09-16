#ifndef SWEETLINE_UTIL_H
#define SWEETLINE_UTIL_H

#include <cstdarg>
#include <cstdint>
#include "sweetline/macro.h"

namespace NS_SWEETLINE {
  /// UTF-8 string utility
  class Utf8Util {
  public:
    Utf8Util() = delete;
    Utf8Util(const Utf8Util&) = delete;
    Utf8Util& operator=(const Utf8Util&) = delete;

    /// Count the number of characters in a UTF-8 string
    /// @param str UTF-8 text
    static size_t countChars(const U8String& str);

    /// Convert character position to byte position
    /// @param str UTF-8 text
    /// @param char_pos Character position
    static size_t charPosToBytePos(const U8String& str, size_t char_pos);

    /// Convert byte position to character position
    /// @param str UTF-8 text
    /// @param byte_pos Byte position
    static size_t bytePosToCharPos(const U8String& str, size_t byte_pos);

    /// Get a UTF-8 substring (by character count)
    /// @param str UTF-8 text
    /// @param start_char Start character position
    /// @param char_count Character count
    /// @return Extracted substring
    static U8String utf8Substr(const U8String& str, size_t start_char, size_t char_count);

    /// Check if a UTF-8 string is valid
    /// @param str UTF-8 text
    static bool isValidUTF8(const U8String& str);
  };

  /// String utility
  class StrUtil {
  public:
    StrUtil() = delete;
    StrUtil(const StrUtil&) = delete;
    StrUtil& operator=(const StrUtil&) = delete;

    /// Convert String to WString
    /// \param s String content
    /// \return Converted wide string
    static std::wstring toWString(const std::string& s);

    /// Convert WString to String
    /// \param ws WString content
    /// \return Converted UTF-8 text
    static std::string toString(const std::wstring& ws);

    /// Convert wide string to C-style string (memory safe)
    /// @param ws Wide string
    static UniquePtr<const char[]> toCString(const std::wstring& ws);

    /// Format string with va_list arguments
    /// @param format Format string
    /// @param args Arguments
    /// @return Formatted text
    static U8String vFormatString(const char* format, va_list args);

    /// Format string
    /// \param format Format string
    /// \param ... Substitution arguments
    /// \return Formatted text
    static U8String formatString(const char* format, ...);

    /// Trim leading and trailing whitespace
    /// \param str Text to trim
    static U8String trim(const U8String& str);

    /// Replace the first occurrence of specified text
    /// @param str Original text
    /// @param from Text to replace
    /// @param to Replacement text
    /// @return Returns true on successful replacement
    static bool replaceFirst(U8String& str, const U8String& from, const U8String& to);

    /// Replace all occurrences of specified text
    /// @param source Original text
    /// @param from Text to replace
    /// @param to Replacement text
    /// @return Replaced text
    static U8String replaceAll(const U8String& source, const U8String& from, const U8String& to);

    /// Check if a string starts with the specified prefix
    /// @param str Text
    /// @param prefix Prefix
    static bool startsWith(const U8String& str, const U8String& prefix);

    /// Check if a string ends with the specified suffix
    /// @param str Text
    /// @param suffix Suffix
    static bool endsWith(const U8String& str, const U8String& suffix);

    /// Check if a string contains the specified substring
    /// @param str Text
    /// @param partial Substring to find
    static bool contains(const U8String& str, const U8String& partial);

    /// Convert GBK encoded text to UTF-8
    /// @param str GBK text
    /// @return UTF-8 text
    static U8String convertGBKToUTF8(const U8String& str);

    /// Convert UTF-8 encoded text to GBK
    /// @param str UTF-8 text
    /// @return GBK text
    static U8String convertUTF8ToGBK(const U8String& str);
  };

  /// Pattern utility
  class PatternUtil {
  public:
    PatternUtil() = delete;
    PatternUtil(const PatternUtil&) = delete;
    PatternUtil& operator=(const PatternUtil&) = delete;

    /// Count the number of capture groups in a pattern
    /// @param pattern_str Pattern string
    /// @param error Receives regex compilation errors
    static int32_t countCaptureGroups(const U8String& pattern_str, U8String& error);

    /// Check if a pattern contains multiline matching
    /// @param pattern_ptr Pattern string
    static bool isMultiLinePattern(const U8String& pattern_ptr);
  };

  /// File utility
  class FileUtil {
  public:
    FileUtil() = delete;
    FileUtil(const FileUtil&) = delete;
    FileUtil& operator=(const FileUtil&) = delete;

    /// Get the file name at the specified path
    /// @param path File path
    /// @return File name (including extension)
    static U8String getPathName(const U8String& path);

    /// Get the file extension at the specified path
    /// @param path File path
    /// @return File extension
    static U8String getExtension(const U8String& path);

    /// Get the parent directory of the specified path
    /// @param path File path
    /// @return Parent directory path
    static U8String getParentPath(const U8String& path);

    /// Check if the file at the specified path exists
    /// @param path File path
    /// @return Returns true if the file exists
    static bool exists(const U8String& path);

    /// Create directories (recursively creates parent directories)
    /// @param path Directory path
    /// @return Returns true on success
    static bool mkdirs(const U8String& path);

    /// Create a single directory (without recursive creation)
    /// @param path Directory path
    /// @return Returns true on success
    static bool mkdir(const U8String& path);

    /// Check if the specified path is a file (not a directory)
    /// @param path File path
    /// @return Returns true if the path exists and is a regular file
    static bool isFile(const U8String& path);

    /// Check if the specified path is a directory (not a file)
    /// @param path File path
    /// @return Returns true if the path exists and is a directory
    static bool isDirectory(const U8String& path);

    /// Read the content of a file
    /// @param path File path
    static U8String readString(const U8String& path);

    /// Write content to a file
    /// @param path File path
    /// @param text Text content
    /// @return Returns true on success
    static bool writeString(const U8String& path, const U8String& text);
  };
}

#endif //SWEETLINE_UTIL_H
