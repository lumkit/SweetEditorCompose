#ifndef SWEETLINE_C_API_H
#define SWEETLINE_C_API_H
#include <stddef.h>
#include <stdint.h>
#include <stdbool.h>

#if defined(SWEETLINE_STATIC)
  #define SL_API
#elif defined(_WIN32) || defined(_WIN64) || defined(WINDOWS)
  #ifdef SWEETLINE_EXPORT
    #define SL_API __declspec(dllexport)
  #else
    #define SL_API __declspec(dllimport)
  #endif
#else
  #define SL_API __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/// Error codes
typedef enum sl_error {
  SL_OK = 0, // No error
  SL_HANDLE_INVALID = 1, // Invalid handle
  SL_JSON_PROPERTY_MISSED = -1, // Missing property in syntax rule JSON
  SL_JSON_PROPERTY_INVALID = -2, // Invalid property value in syntax rule JSON
  SL_PATTERN_INVALID = -3, // Invalid regex pattern in syntax rule JSON
  SL_STATE_INVALID = -4, // Invalid state in syntax rule JSON
  SL_JSON_INVALID = -5, // Invalid syntax rule JSON
  SL_FILE_NOT_EXISTS = -6, // Syntax rule file does not exist
  SL_FILE_INVALID = -7, // Syntax rule file content is invalid or empty
  SL_IMPORT_SYNTAX_NOT_FOUND = -8, // importSyntax target syntax is not compiled yet
  SL_STATE_REFERENCE_NOT_FOUND = -9, // state/subState/onLineEndState target does not exist
  SL_INLINE_STYLE_REFERENCE_NOT_FOUND = -10, // inline style name is not declared in styles[]
} sl_error_t;

/// Syntax rule error information
typedef struct sl_syntax_error {
  /// Error code
  sl_error_t err_code;
  /// Error message
  const char* err_msg;
} sl_syntax_error_t;

/// Managed document handle
typedef struct sl_document_handle sl_document_handle;
typedef sl_document_handle* sl_document_handle_t;
/// Highlight engine handle
typedef struct sl_engine_handle sl_engine_handle;
typedef sl_engine_handle* sl_engine_handle_t;
/// Highlight analyzer handle
typedef struct sl_analyzer_handle sl_analyzer_handle;
typedef sl_analyzer_handle* sl_analyzer_handle_t;

/// Create a managed document (Document)
/// @param uri Document URI
/// @param text Document content
/// @return Managed document handle
SL_API sl_document_handle_t sl_create_document(const char* uri, const char* text);

/// Destroy a managed document
/// @param document_handle Managed document handle
/// @return Error code, see @see {sl_error_t}. Returns @see {SL_OK} on success
SL_API sl_error_t sl_free_document(sl_document_handle_t document_handle);

/// Create a SweetLine highlight engine
/// @param show_index Whether the analysis result includes character index, if not only line and column are returned
/// @param inline_style Whether the analysis result uses inline styles instead of only returning style IDs
/// @param tab_size Tab width used for indent guide level calculation
/// @return Highlight engine handle
SL_API sl_engine_handle_t sl_create_engine(bool show_index, bool inline_style, int32_t tab_size);

/// Destroy the highlight engine
/// @param engine_handle Highlight engine handle
/// @return Error code, returns @see {SL_OK} on success
SL_API sl_error_t sl_free_engine(sl_engine_handle_t engine_handle);

/// Define a macro for controlling #ifdef conditional compilation in importSyntax
/// @param engine_handle Highlight engine handle
/// @param macro_name Macro name
/// @return Error code, returns @see {SL_OK} on success
SL_API sl_error_t sl_engine_define_macro(sl_engine_handle_t engine_handle, const char* macro_name);

/// Undefine a macro
/// @param engine_handle Highlight engine handle
/// @param macro_name Macro name
/// @return Error code, returns @see {SL_OK} on success
SL_API sl_error_t sl_engine_undefine_macro(sl_engine_handle_t engine_handle, const char* macro_name);

/// Compile syntax rules (directly from JSON content)
/// @param engine_handle Highlight engine handle
/// @param syntax_json Syntax rule JSON configuration
/// @return Syntax rule error information (if any)
SL_API sl_syntax_error_t sl_engine_compile_json(sl_engine_handle_t engine_handle, const char* syntax_json);

/// Compile syntax rules (from a JSON configuration file)
/// @param engine_handle Highlight engine handle
/// @param syntax_file Path to the syntax rule JSON file
/// @return Syntax rule error information (if any)
SL_API sl_syntax_error_t sl_engine_compile_file(sl_engine_handle_t engine_handle, const char* syntax_file);

/// Register a style name mapping from style name to ID
/// @param engine_handle Highlight engine handle
/// @param style_name Highlight style name
/// @param style_id Highlight style ID
/// @return Error code, returns @see {SL_OK} on success
SL_API sl_error_t sl_engine_register_style_name(sl_engine_handle_t engine_handle, const char* style_name, int32_t style_id);

/// Get style name by style ID
/// @param engine_handle Highlight engine handle
/// @param style_id Highlight style ID
/// @return The registered style name for the given ID in the engine
SL_API const char* sl_engine_get_style_name(sl_engine_handle_t engine_handle, int32_t style_id);

/// Create a plain text highlight analyzer by syntax rule name (no incremental analysis support)
/// @param engine_handle Highlight engine handle
/// @param syntax_name Syntax rule name
/// @return Plain text highlight analyzer handle
SL_API sl_analyzer_handle_t sl_engine_create_text_analyzer(sl_engine_handle_t engine_handle, const char* syntax_name);

/// Create a plain text highlight analyzer by file name or path (no incremental analysis support)
/// @param engine_handle Highlight engine handle
/// @param file_name File name or path
/// @return Plain text highlight analyzer handle
SL_API sl_analyzer_handle_t sl_engine_create_text_analyzer_by_file_name(
  sl_engine_handle_t engine_handle, const char* file_name);

/// Perform full highlight analysis on a text
/// @param analyzer_handle Plain text highlight analyzer handle
/// @param text Full text content
/// @return Analysis result, tightly packed in byte order. Structure:
/// @code
/// result[0] = span payload flags
///             bit0: hasStartIndex
///             bit1: inlineStyle
/// result[1] = span field count (stride)
/// result[2] = line count
/// Followed by line_count line entries:
///   line_entry[0] = span count of current line
///   Followed by span_count * stride fields
/// Span payload:
///   common: [column, length]
///   if show_index=true: append [startIndex]
///   if inline_style=true: append [foregroundColor, backgroundColor, fontAttributes]
///   else: append [styleId]
/// fontAttributes bitmask:
///   (bits & 1) != 0 => bold
///   (bits & (1 << 1)) != 0 => italic
///   (bits & (1 << 2)) != 0 => strikethrough
/// @endcode
/// Note: the return value must be freed by calling sl_free_buffer after use
SL_API int32_t* sl_text_analyze(sl_analyzer_handle_t analyzer_handle, const char* text);

/// Perform single line highlight analysis and return the result
/// @param analyzer_handle Plain text highlight analyzer handle
/// @param text Single line text content
/// @param line_info Metadata for the current line, must be an int32_t array of length 3:
/// @code
/// line_info[0] = current line number
/// line_info[1] = start highlight state, typically from the end_state of the previous line's result; line 0 starts at 0
/// line_info[2] = cumulative character count up to the current line, excluding line endings
/// @endcode
/// @return Analysis result, tightly packed in byte order. Structure:
/// @code
/// result[0] = span payload flags
///             bit0: hasStartIndex
///             bit1: inlineStyle
/// result[1] = number of integer fields per token span
/// result[2] = number of token spans
/// result[3] = state ID at the end of current line analysis
/// result[4] = total characters analyzed in current line (excluding line endings)
/// Followed by result[2] * result[1] integer fields (single-line span payload):
///   common: [column, length]
///   if show_index=true: append [startIndex]
///   if inline_style=true: append [foregroundColor, backgroundColor, fontAttributes]
///   else: append [styleId]
/// @endcode
/// Note: the return value must be freed by calling sl_free_buffer after use
SL_API int32_t* sl_text_analyze_line(sl_analyzer_handle_t analyzer_handle, const char* text, int32_t* line_info);

/// Perform indent guide analysis on plain text
/// @param analyzer_handle Plain text analyzer handle
/// @param text Text content
/// @return Analysis result, format same as sl_document_analyze_indent_guides
/// Structure:
/// @code
/// result[0] = slice start line
/// result[1] = number of line states
/// result[2] = number of indent guide lines
/// Followed by guide line entries:
/// [column, start_line, end_line, flags, branch_count, branch_line_0, branch_column_0, ...]
/// flags:
///   bit0: continuesBefore
///   bit1: continuesAfter
/// Followed by line state entries:
/// [nesting_level, scope_state, scope_column, indent_level]
/// where scope_state: 0=START, 1=END, 2=CONTENT
/// @endcode
/// Note: the return value must be freed by calling sl_free_buffer after use
SL_API int32_t* sl_text_analyze_indent_guides(sl_analyzer_handle_t analyzer_handle, const char* text);

/// Perform bracket pair analysis on plain text
/// @param analyzer_handle Plain text analyzer handle
/// @param text Text content
/// @return Analysis result, tightly packed in byte order. Structure:
/// @code
/// result[0] = bracket payload flags
///             bit0: hasStartIndex
/// result[1] = bracket token field count (stride)
/// result[2] = line count
/// Followed by line_count line entries:
///   line_entry[0] = bracket token count of current line
///   followed by token_count * stride fields
/// Bracket token payload:
///   common: [column, length, depth, kind, matchState, partnerLine, partnerColumn, partnerLength]
///   if show_index=true: [column, length, startIndex, depth, kind, matchState,
///                        partnerLine, partnerColumn, partnerLength, partnerStartIndex]
/// where kind: 0=OPEN, 1=CLOSE
/// where matchState: 0=MATCHED, 1=UNMATCHED, 2=UNKNOWN
/// partner fields are -1 when no known partner exists
/// @endcode
/// Note: the return value must be freed by calling sl_free_buffer after use
SL_API int32_t* sl_text_analyze_bracket_pairs(sl_analyzer_handle_t analyzer_handle, const char* text);

/// Destroy a plain text analyzer handle
/// @param analyzer_handle Analyzer handle returned by sl_engine_create_text_analyzer or
/// sl_engine_create_text_analyzer_by_file_name
/// @return Error code, returns @see {SL_OK} on success
SL_API sl_error_t sl_free_text_analyzer(sl_analyzer_handle_t analyzer_handle);

/// Load a managed document and get a document highlight analyzer handle (supports incremental analysis)
/// @param engine_handle Highlight engine handle
/// @param document_handle Managed document handle
/// @return Document highlight analyzer handle
SL_API sl_analyzer_handle_t sl_engine_load_document(sl_engine_handle_t engine_handle, sl_document_handle_t document_handle);

/// Remove a previously loaded managed document from the engine
/// @param engine_handle Highlight engine handle
/// @param uri URI of the managed document
/// @return Error code, returns @see {SL_OK} on success
SL_API sl_error_t sl_engine_remove_document(sl_engine_handle_t engine_handle, const char* uri);

/// Perform full highlight analysis on a managed document (typically called once after initial document load)
/// @param analyzer_handle Document highlight analyzer handle
/// @return Analysis result, tightly packed in byte order. Structure:
/// @code
/// Same format as sl_text_analyze:
/// [flags, spanStride, lineCount, lineEntry...]
/// @endcode
/// Note: the return value must be freed by calling sl_free_buffer after use
SL_API int32_t* sl_document_analyze(sl_analyzer_handle_t analyzer_handle);

/// Analyze enough lines to cover the specified line range on a managed document
/// @param analyzer_handle Document highlight analyzer handle
/// @param visible_range Visible line range, array structure: [startLine],[lineCount]
/// @return Highlight slice for the specified line range, tightly packed in byte order. Structure:
/// @code
/// Same format as sl_document_analyze_incremental_in_line_range:
/// [flags, spanStride, startLine, totalLineCount, lineCount, lineEntry...]
/// @endcode
/// Note: the return value must be freed by calling sl_free_buffer after use
SL_API int32_t* sl_document_analyze_line_range(sl_analyzer_handle_t analyzer_handle, int32_t* visible_range);

/// Perform incremental highlight analysis on a managed document (called when document changes)
/// @param analyzer_handle Document highlight analyzer handle
/// @param changes_range Change range, array structure: [startLine],[startColumn],[endLine],[endColumn]
/// @param new_text Changed text
/// @return Full analysis result for the entire document, tightly packed in byte order. Structure:
/// @code
/// Same format as sl_text_analyze:
/// [flags, spanStride, lineCount, lineEntry...]
/// @endcode
/// Note: the return value must be freed by calling sl_free_buffer after use
SL_API int32_t* sl_document_analyze_incremental(sl_analyzer_handle_t analyzer_handle, int32_t* changes_range, const char* new_text);

/// Perform incremental highlight analysis on a managed document, returning only a highlight slice for the specified line range
/// @param analyzer_handle Document highlight analyzer handle
/// @param changes_range Change range, array structure: [startLine],[startColumn],[endLine],[endColumn]
/// @param new_text Changed text
/// @param visible_range Visible line range, array structure: [startLine],[lineCount]
/// @return Highlight slice for the specified line range, tightly packed in byte order. Structure:
/// @code
/// result[0] = span payload flags
///             bit0: hasStartIndex
///             bit1: inlineStyle
/// result[1] = span field count (stride)
/// result[2] = slice start line (start_line)
/// result[3] = total line count after patch (total_line_count)
/// result[4] = slice line count (line_count)
/// Followed by line_count line entries:
///   line_entry[0] = span count of current line
///   followed by span payloads (same span payload structure as sl_text_analyze)
/// @endcode
/// Note: the return value must be freed by calling sl_free_buffer after use
SL_API int32_t* sl_document_analyze_incremental_in_line_range(
  sl_analyzer_handle_t analyzer_handle, int32_t* changes_range, const char* new_text, int32_t* visible_range);

/// Get highlight slice from the current cached document highlight result without triggering new analysis
/// @param analyzer_handle Document highlight analyzer handle
/// @param visible_range Visible line range, array structure: [startLine],[lineCount]
/// @return Highlight slice for the specified line range, tightly packed in byte order. Structure:
/// @code
/// Same format as sl_document_analyze_incremental_in_line_range:
/// [flags, spanStride, startLine, totalLineCount, lineCount, lineEntry...]
/// @endcode
/// Note: the return value must be freed by calling sl_free_buffer after use
SL_API int32_t* sl_document_get_highlight_slice(sl_analyzer_handle_t analyzer_handle, int32_t* visible_range);

/// Perform indent guide analysis on a managed document
/// @param analyzer_handle Document analyzer handle
/// @return Analysis result, tightly packed in byte order. Structure:
/// @code
/// result[0] = slice start line
/// result[1] = number of line states
/// result[2] = number of indent guide lines
/// Followed by guide line entries:
/// [column, start_line, end_line, flags, branch_count, branch_line_0, branch_column_0, ...]
/// flags:
///   bit0: continuesBefore
///   bit1: continuesAfter
/// Followed by line state entries:
/// [nesting_level, scope_state, scope_column, indent_level]
/// where scope_state: 0=START, 1=END, 2=CONTENT
/// @endcode
/// Note: the return value must be freed by calling sl_free_buffer after use
SL_API int32_t* sl_document_analyze_indent_guides(sl_analyzer_handle_t analyzer_handle);

/// Perform indent guide analysis for the requested visible line range on a managed document
/// @param analyzer_handle Document analyzer handle
/// @param visible_range Visible line range, array structure: [startLine],[lineCount]
/// @return Analysis result, format same as sl_document_analyze_indent_guides
/// Note: the return value must be freed by calling sl_free_buffer after use
SL_API int32_t* sl_document_analyze_indent_guides_in_line_range(
  sl_analyzer_handle_t analyzer_handle, int32_t* visible_range);

/// Perform bracket pair analysis on a managed document
/// @param analyzer_handle Document analyzer handle
/// @return Analysis result, tightly packed in byte order. Structure:
/// @code
/// Same format as sl_text_analyze_bracket_pairs:
/// [flags, bracketTokenStride, lineCount, lineEntry...]
/// @endcode
/// Note: the return value must be freed by calling sl_free_buffer after use
SL_API int32_t* sl_document_analyze_bracket_pairs(sl_analyzer_handle_t analyzer_handle);

/// Perform bracket pair analysis for the requested visible line range on a managed document
/// @param analyzer_handle Document analyzer handle
/// @param visible_range Visible line range, array structure: [startLine],[lineCount]
/// @return Analysis result, tightly packed in byte order. Structure:
/// @code
/// result[0] = bracket payload flags
///             bit0: hasStartIndex
/// result[1] = bracket token field count (stride)
/// result[2] = slice start line
/// result[3] = total line count
/// result[4] = slice line count
/// Followed by line_count line entries:
///   line_entry[0] = bracket token count of current line
///   followed by token_count * stride fields
/// Bracket token payload is the same as sl_text_analyze_bracket_pairs.
/// @endcode
/// Note: the return value must be freed by calling sl_free_buffer after use
SL_API int32_t* sl_document_analyze_bracket_pairs_in_line_range(
  sl_analyzer_handle_t analyzer_handle, int32_t* visible_range);

/// Destroy a document analyzer handle
/// @param analyzer_handle Analyzer handle returned by sl_engine_load_document
/// @return Error code, returns @see {SL_OK} on success
SL_API sl_error_t sl_free_document_analyzer(sl_analyzer_handle_t analyzer_handle);

/// Free the memory of analysis results. All analysis functions returning int32_t*
/// (such as sl_text_analyze, sl_document_analyze, sl_document_analyze_incremental,
/// sl_document_analyze_incremental_in_line_range, sl_document_get_highlight_slice) must be freed via this function
/// @param result Highlight analysis result
SL_API void sl_free_buffer(int32_t* result);

#ifdef __cplusplus
}
#endif

#endif //SWEETLINE_C_API_H
