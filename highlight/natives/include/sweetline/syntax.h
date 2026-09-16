#ifndef SWEETLINE_SYNTAX_H
#define SWEETLINE_SYNTAX_H

#include "sweetline/macro.h"

namespace NS_SWEETLINE {
  /// Error thrown during syntax rule compilation
  class SyntaxCompileError : public std::exception {
  public:
    /// Missing property
    static constexpr int ERR_JSON_PROPERTY_MISSED = -1;
    /// Invalid property value
    static constexpr int ERR_JSON_PROPERTY_INVALID = -2;
    /// Invalid regex pattern
    static constexpr int ERR_PATTERN_INVALID = -3;
    /// Invalid state
    static constexpr int ERR_STATE_INVALID = -4;
    /// JSON syntax error
    static constexpr int ERR_JSON_INVALID = -5;
    /// File not found
    static constexpr int ERR_FILE_NOT_EXISTS = -6;
    /// Empty file content
    static constexpr int ERR_FILE_INVALID = -7;
    /// Imported syntax rule not found
    static constexpr int ERR_IMPORT_SYNTAX_NOT_FOUND = -8;
    /// Referenced state/subState/onLineEndState not found
    static constexpr int ERR_STATE_REFERENCE_NOT_FOUND = -9;
    /// Referenced inline style name not found in styles[]
    static constexpr int ERR_INLINE_STYLE_REFERENCE_NOT_FOUND = -10;

    explicit SyntaxCompileError(int err_code);
    explicit SyntaxCompileError(int err_code, const U8String& message);
    explicit SyntaxCompileError(int err_code, const char* message);

    const char* what() const noexcept override;
    const U8String& message() const noexcept;
    int code() const noexcept;
  private:
    int m_err_code_;
    U8String m_message_;
  };

  /// Bold style
  constexpr static int16_t kStyleBold = 1;
  /// Italic style
  constexpr static int16_t kStyleItalic = kStyleBold << 1;
  /// Strikethrough style
  constexpr static int16_t kStyleStrikeThrough = kStyleItalic << 1;

  /// Inline style definition embedded in syntax rules
  struct InlineStyle {
    /// Foreground color (ARGB)
    int32_t foreground {0};
    /// Background color (ARGB)
    int32_t background {0};
    /// Whether to display in bold
    bool is_bold {false};
    /// Whether to display in italic
    bool is_italic {false};
    /// Whether to display with strikethrough
    bool is_strikethrough {false};
  };

  /// Mapping between highlight style IDs and names
  struct StyleMapping {
    StyleMapping();

    /// Map from style name to ID
    HashMap<U8String, int32_t> style_name_id_map;
    /// Map from style ID to name
    HashMap<int32_t, U8String> style_id_name_map;

    void registerStyleName(const U8String& style_name, int32_t style_id);
    int32_t getStyleId(const U8String& style_name);
    int32_t getOrCreateStyleId(const U8String& style_name);
    const U8String& getStyleName(int32_t style_id);
  private:
    int32_t m_style_id_counter_ {1};
    static int32_t kDefaultStyleId;
    static U8String kDefaultStyleName;
  };

  struct StateRule;
  struct ScopeRule;
  struct ScopeSkipRule;
  struct BracketRule;
  struct SyntaxRuleRuntimeData;
  class SyntaxRuleCompiler;
  /// Syntax rule definition
  struct SyntaxRule {
    /// Name of the syntax rule
    U8String name;
    /// Supported exact file names
    HashSet<U8String> file_names;
    /// Supported file suffixes
    HashSet<U8String> file_suffixes;
    /// Supported file name regex patterns
    List<U8String> file_name_patterns;
    /// Inline styles defined in syntax rules (nullable), style ID -> InlineStyle
    HashMap<int32_t, InlineStyle> inline_styles;
    /// Inline style mapping table (inline_style mode)
    UniquePtr<StyleMapping> style_mapping;
    /// Variables
    HashMap<U8String, U8String> variables_map;
    /// Map from state ID to StateRule
    HashMap<int32_t, StateRule> state_rules_map;
    /// Map from state name to ID
    HashMap<U8String, int32_t> state_id_map;
    /// Scope rules used by indent guide analysis
    List<ScopeRule> scope_rules;
    /// Lexical skip rules used by scope analysis
    List<ScopeSkipRule> scope_skip_rules;
    /// Bracket pair rules used by bracket pair analysis
    List<BracketRule> bracket_rules;
    /// Lexical skip rules used by bracket pair analysis
    List<ScopeSkipRule> bracket_skip_rules;

    bool containsInlineStyle(int32_t style_id);
    InlineStyle& getInlineStyle(int32_t style_id);

    int32_t getOrCreateStateId(const U8String& state_name);
    bool containsRule(int32_t state_id) const;
    StateRule& getStateRule(int32_t state_id);
    bool matchesFileNamePattern(const U8String& file_name, size_t index) const;

    SyntaxRule();
    ~SyntaxRule();
    SyntaxRule(const SyntaxRule&) = delete;
    SyntaxRule& operator=(const SyntaxRule&) = delete;
    SyntaxRule(SyntaxRule&&) = delete;
    SyntaxRule& operator=(SyntaxRule&&) = delete;

    constexpr static int32_t kDefaultStateId = 0;
    constexpr static const char* kDefaultStateName = "default";
#ifdef SWEETLINE_DEBUG
    void dump() const;
#endif
  private:
    int32_t m_state_id_counter_ {1};
    UniquePtr<SyntaxRuleRuntimeData> m_runtime_data_;

    friend class SyntaxRuleCompiler;
  };
}

#endif //SWEETLINE_SYNTAX_H
