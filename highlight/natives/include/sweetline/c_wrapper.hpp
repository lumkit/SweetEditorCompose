#ifndef SWEETLINE_C_WRAPPER_HPP
#define SWEETLINE_C_WRAPPER_HPP

#include "sweetline/c_sweetline.h"
#include "sweetline/highlight.h"

using namespace NS_SWEETLINE;

class StringKeepAlive {
public:
  static StringKeepAlive& getInstance();
  void clear();

  template<typename U>
  const char* getAliveCString(U&& str) {
    vector_.push_back(std::forward<U>(str));
    return vector_.back().c_str();
  }
private:
  std::vector<U8String> vector_;
};

template<typename T>
class CPtrHolder {
public:
  explicit CPtrHolder(const SharedPtr<T>& ptr): m_ptr_(ptr) {
  }

  SharedPtr<T>& get() {
    return m_ptr_;
  }
private:
  SharedPtr<T> m_ptr_;
};

template<typename HandleT, typename ClassT, typename... Args>
HandleT makeCPtrHolderToHandle(Args&&... args) {
  SharedPtr<ClassT> ptr = makeSharedPtr<ClassT>((std::forward<Args>(args))...);
  CPtrHolder<ClassT>* holder = new CPtrHolder<ClassT>(ptr);
  return reinterpret_cast<HandleT>(holder);
}

template<typename HandleT, typename ClassT>
HandleT asCHandle(const SharedPtr<ClassT>& ptr_v) {
  if (ptr_v == nullptr) {
    return HandleT{};
  }
  CPtrHolder<ClassT>* holder = new CPtrHolder<ClassT>(ptr_v);
  return reinterpret_cast<HandleT>(holder);
}

template<typename HandleT, typename ClassT>
SharedPtr<ClassT> getCPtrHolderValue(HandleT handle) {
  if (handle == 0) {
    return nullptr;
  }
  CPtrHolder<ClassT>* holder = reinterpret_cast<CPtrHolder<ClassT>*>(handle);
  if (holder != nullptr) {
    return holder->get();
  } else {
    return nullptr;
  }
}

template<typename HandleT, typename ClassT>
void deleteCPtrHolder(HandleT handle) {
  if (handle == 0) {
    return;
  }
  CPtrHolder<ClassT>* holder = reinterpret_cast<CPtrHolder<ClassT>*>(handle);
  if (holder != nullptr) {
    delete holder;
  }
}

inline int32_t packHighlightConfig(const HighlightConfig& config) {
  int32_t bits = 0;
  if (config.show_index) {
    bits |= 1;
  }
  if (config.inline_style) {
    bits |= 1 << 1;
  }
  // Encode tab_size into bit8~bit15 (8 bits, supports 0~255)
  bits |= (config.tab_size & 0xFF) << 8;
  return bits;
}

inline HighlightConfig unpackHighlightConfig(int32_t bits) {
  HighlightConfig config;
  if ((bits & 1) != 0) {
    config.show_index = true;
  }
  if ((bits & (1 << 1)) != 0) {
    config.inline_style = true;
  }
  int32_t tab_size = (bits >> 8) & 0xFF;
  if (tab_size > 0) {
    config.tab_size = tab_size;
  }
  return config;
}

inline int32_t packInlineStyleTags(const InlineStyle& style) {
  int32_t bits = 0;
  if (style.is_bold) {
    bits |= kStyleBold;
  }
  if (style.is_italic) {
    bits |= kStyleItalic;
  }
  if (style.is_strikethrough) {
    bits |= kStyleStrikeThrough;
  }
  return bits;
}

inline int64_t packTextPosition(const TextPosition& position) {
  int64_t line = (int64_t)position.line;
  int64_t column = (int64_t)position.column;
  return (line << 32) | (column & 0XFFFFFFFFLL);
}

inline TextPosition unpackTextPosition(int64_t bits) {
  size_t start_line = (size_t)(int32_t)(bits >> 32);
  size_t start_column = (size_t)(int32_t)(bits & 0XFFFFFFFF);
  return {start_line, start_column};
}

inline TextLineInfo unpackTextLineInfo(int32_t* arr) {
  return {static_cast<size_t>(arr[0]), arr[1], static_cast<size_t>(arr[2])};
}

inline int32_t computeSpanBufferStride(const HighlightConfig& config) {
  int32_t base_count = 2; // column, length
  if (config.show_index) {
    base_count += 1; // start index
  }
  if (config.inline_style) {
    base_count += 3; // foreground color, background color, tags each occupy one int32
  } else {
    base_count += 1; // styleId occupies one int32
  }
  return base_count;
}

inline int32_t packSpanPayloadFlags(const HighlightConfig& config) {
  int32_t flags = 0;
  if (config.show_index) {
    flags |= 1; // bit0: has start index
  }
  if (config.inline_style) {
    flags |= (1 << 1); // bit1: inline style mode
  }
  return flags;
}

inline int32_t computeBracketTokenStride(const HighlightConfig& config) {
  int32_t base_count = 8; // column, length, depth, kind, match state, partner line, partner column, partner length
  if (config.show_index) {
    base_count += 2; // start index and partner start index
  }
  return base_count;
}

inline int32_t packBracketPayloadFlags(const HighlightConfig& config) {
  int32_t flags = 0;
  if (config.show_index) {
    flags |= 1; // bit0: has start index
  }
  return flags;
}

inline size_t computeDocumentHighlightBufferSize(const SharedPtr<DocumentHighlight>& highlight, const HighlightConfig& config) {
  size_t total_size = 3; // flags + span_stride + line_count
  if (highlight == nullptr) {
    return total_size;
  }
  size_t stride = static_cast<size_t>(computeSpanBufferStride(config));
  for (const LineHighlight& line : highlight->lines) {
    total_size += 1 + line.spans.size() * stride; // line span_count + span payload
  }
  return total_size;
}

inline size_t computeDocumentHighlightSliceBufferSize(const SharedPtr<DocumentHighlightSlice>& slice, const HighlightConfig& config) {
  size_t total_size = 5; // flags + span_stride + start_line + total_line_count + line_count
  if (slice == nullptr) {
    return total_size;
  }
  size_t stride = static_cast<size_t>(computeSpanBufferStride(config));
  for (const LineHighlight& line : slice->lines) {
    total_size += 1 + line.spans.size() * stride; // line span_count + span payload
  }
  return total_size;
}

inline size_t computeIndentGuideResultBufferSize(const SharedPtr<IndentGuideResult>& result) {
  size_t total_size = 3; // start_line + line_state_count + guide_count
  if (result == nullptr) {
    return total_size;
  }
  size_t guide_data_size = 0;
  for (const IndentGuideLine& g : result->guide_lines) {
    guide_data_size += 5 + g.branches.size() * 2; // fixed guide fields + variable branches
  }
  total_size += guide_data_size + result->line_states.size() * 4;
  return total_size;
}

inline size_t computeBracketPairResultBufferSize(const SharedPtr<BracketPairResult>& result,
                                                 const HighlightConfig& config,
                                                 bool slice) {
  size_t total_size = slice ? 5 : 3;
  if (result == nullptr) {
    return total_size;
  }
  size_t stride = static_cast<size_t>(computeBracketTokenStride(config));
  for (const LineBracketPairs& line : result->lines) {
    total_size += 1 + line.tokens.size() * stride;
  }
  return total_size;
}

inline void writeTokenSpanCompact(const TokenSpan& span, int32_t* buffer, size_t& index, const HighlightConfig& config) {
  size_t length = span.range.end.column - span.range.start.column;
  buffer[index++] = static_cast<int32_t>(span.range.start.column);
  buffer[index++] = static_cast<int32_t>(length);
  if (config.show_index) {
    buffer[index++] = static_cast<int32_t>(span.range.start.index);
  }
  if (config.inline_style) {
    buffer[index++] = static_cast<int32_t>(span.inline_style.foreground);
    buffer[index++] = static_cast<int32_t>(span.inline_style.background);
    buffer[index++] = packInlineStyleTags(span.inline_style);
  } else {
    buffer[index++] = static_cast<int32_t>(span.style_id);
  }
}

inline void writeDocumentHighlight(const SharedPtr<DocumentHighlight>& highlight, int32_t* buffer, const HighlightConfig& config) {
  size_t index = 0;
  size_t line_count = highlight == nullptr ? 0 : highlight->lines.size();
  buffer[index++] = packSpanPayloadFlags(config);
  buffer[index++] = computeSpanBufferStride(config);
  buffer[index++] = static_cast<int32_t>(line_count);
  if (highlight == nullptr) {
    return;
  }
  for (const LineHighlight& line : highlight->lines) {
    buffer[index++] = static_cast<int32_t>(line.spans.size());
    for (const TokenSpan& span : line.spans) {
      writeTokenSpanCompact(span, buffer, index, config);
    }
  }
}

inline void writeLineHighlight(const LineHighlight& highlight, int32_t* buffer, const HighlightConfig& config) {
  size_t index = 0;
  for (const TokenSpan& span : highlight.spans) {
    writeTokenSpanCompact(span, buffer, index, config);
  }
}

inline void writeDocumentHighlightSlice(const SharedPtr<DocumentHighlightSlice>& slice, int32_t* buffer,
                                        const HighlightConfig& config) {
  size_t index = 0;
  buffer[index++] = packSpanPayloadFlags(config);
  buffer[index++] = computeSpanBufferStride(config);
  buffer[index++] = static_cast<int32_t>(slice == nullptr ? 0 : slice->start_line);
  buffer[index++] = static_cast<int32_t>(slice == nullptr ? 0 : slice->total_line_count);
  buffer[index++] = static_cast<int32_t>(slice == nullptr ? 0 : slice->lines.size());
  if (slice == nullptr) {
    return;
  }
  for (const LineHighlight& line : slice->lines) {
    buffer[index++] = static_cast<int32_t>(line.spans.size());
    for (const TokenSpan& span : line.spans) {
      writeTokenSpanCompact(span, buffer, index, config);
    }
  }
}

inline void writeIndentGuideResult(const SharedPtr<IndentGuideResult>& result, int32_t* buffer) {
  size_t line_state_count = result == nullptr ? 0 : result->line_states.size();
  buffer[0] = static_cast<int32_t>(result == nullptr ? 0 : result->start_line);
  buffer[1] = static_cast<int32_t>(line_state_count);
  buffer[2] = static_cast<int32_t>(result == nullptr ? 0 : result->guide_lines.size());
  if (result == nullptr) {
    return;
  }
  size_t idx = 3;
  for (const IndentGuideLine& g : result->guide_lines) {
    int32_t flags = 0;
    if (g.continues_before) {
      flags |= 1;
    }
    if (g.continues_after) {
      flags |= 1 << 1;
    }
    buffer[idx++] = g.column;
    buffer[idx++] = g.start_line;
    buffer[idx++] = g.end_line;
    buffer[idx++] = flags;
    buffer[idx++] = static_cast<int32_t>(g.branches.size());
    for (const auto& bp : g.branches) {
      buffer[idx++] = bp.line;
      buffer[idx++] = bp.column;
    }
  }
  for (const LineScopeState& ls : result->line_states) {
    buffer[idx++] = ls.nesting_level;
    buffer[idx++] = static_cast<int32_t>(ls.scope_state);
    buffer[idx++] = ls.scope_column;
    buffer[idx++] = ls.indent_level;
  }
}

inline void writeBracketTokenCompact(const BracketToken& token, int32_t* buffer, size_t& index,
                                     const HighlightConfig& config) {
  int32_t length = static_cast<int32_t>(token.range.end.column - token.range.start.column);
  bool matched = token.match_state == BracketMatchState::MATCHED;
  buffer[index++] = static_cast<int32_t>(token.range.start.column);
  buffer[index++] = length;
  if (config.show_index) {
    buffer[index++] = static_cast<int32_t>(token.range.start.index);
  }
  buffer[index++] = token.depth;
  buffer[index++] = static_cast<int32_t>(token.kind);
  buffer[index++] = static_cast<int32_t>(token.match_state);
  buffer[index++] = matched ? static_cast<int32_t>(token.partner_range.start.line) : -1;
  buffer[index++] = matched ? static_cast<int32_t>(token.partner_range.start.column) : -1;
  buffer[index++] = matched ? static_cast<int32_t>(token.partner_range.end.column - token.partner_range.start.column) : -1;
  if (config.show_index) {
    buffer[index++] = matched ? static_cast<int32_t>(token.partner_range.start.index) : -1;
  }
}

inline void writeBracketPairResult(const SharedPtr<BracketPairResult>& result, int32_t* buffer,
                                   const HighlightConfig& config) {
  size_t index = 0;
  buffer[index++] = packBracketPayloadFlags(config);
  buffer[index++] = computeBracketTokenStride(config);
  buffer[index++] = static_cast<int32_t>(result == nullptr ? 0 : result->lines.size());
  if (result == nullptr) {
    return;
  }
  for (const LineBracketPairs& line : result->lines) {
    buffer[index++] = static_cast<int32_t>(line.tokens.size());
    for (const BracketToken& token : line.tokens) {
      writeBracketTokenCompact(token, buffer, index, config);
    }
  }
}

inline void writeBracketPairResultSlice(const SharedPtr<BracketPairResult>& result, int32_t* buffer,
                                        const HighlightConfig& config) {
  size_t index = 0;
  buffer[index++] = packBracketPayloadFlags(config);
  buffer[index++] = computeBracketTokenStride(config);
  buffer[index++] = static_cast<int32_t>(result == nullptr ? 0 : result->start_line);
  buffer[index++] = static_cast<int32_t>(result == nullptr ? 0 : result->total_line_count);
  buffer[index++] = static_cast<int32_t>(result == nullptr ? 0 : result->lines.size());
  if (result == nullptr) {
    return;
  }
  for (const LineBracketPairs& line : result->lines) {
    buffer[index++] = static_cast<int32_t>(line.tokens.size());
    for (const BracketToken& token : line.tokens) {
      writeBracketTokenCompact(token, buffer, index, config);
    }
  }
}

#endif //SWEETLINE_C_WRAPPER_HPP
