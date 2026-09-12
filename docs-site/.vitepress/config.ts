import { defineConfig } from "vitepress";

const guideEn = [
  { text: "Install", link: "/guide/install" },
  { text: "Hello World", link: "/guide/quick-start" },
  { text: "Architecture", link: "/guide/architecture" },
  { text: "Platforms", link: "/guide/platforms" },
  { text: "Syntax highlights", link: "/guide/highlights" },
  { text: "Decoration providers", link: "/guide/decoration-provider" },
  { text: "Completion", link: "/guide/completion" },
  { text: "Menus", link: "/guide/menus" },
  { text: "Inline suggestions", link: "/guide/inline-suggestion" },
];

const apiEn = [
  { text: "SweetEditor", link: "/api/sweet-editor" },
  { text: "Controller", link: "/api/controller" },
  { text: "Document and edits", link: "/api/document-edit" },
  { text: "Caret, selection, scroll", link: "/api/caret-selection" },
  { text: "Appearance", link: "/api/appearance" },
  { text: "Events", link: "/api/events" },
  { text: "Spans", link: "/api/decorations-spans" },
  { text: "Completion API", link: "/api/completion" },
  { text: "Search", link: "/api/search" },
  { text: "Folding", link: "/api/folding" },
  { text: "Language and brackets", link: "/api/language" },
  { text: "Key map", link: "/api/keymap" },
  { text: "Overlays", link: "/api/overlays" },
  { text: "Guides", link: "/api/guides" },
  { text: "Diff", link: "/api/diff" },
  { text: "Linked editing", link: "/api/linked-editing" },
  { text: "New-line actions", link: "/api/newline" },
];

const guideZh = [
  { text: "安装", link: "/zh/guide/install" },
  { text: "快速开始", link: "/zh/guide/quick-start" },
  { text: "架构约定", link: "/zh/guide/architecture" },
  { text: "平台注意", link: "/zh/guide/platforms" },
  { text: "语法高亮", link: "/zh/guide/highlights" },
  { text: "装饰 Provider", link: "/zh/guide/decoration-provider" },
  { text: "补全", link: "/zh/guide/completion" },
  { text: "菜单", link: "/zh/guide/menus" },
  { text: "内联建议", link: "/zh/guide/inline-suggestion" },
];

const apiZh = [
  { text: "SweetEditor", link: "/zh/api/sweet-editor" },
  { text: "Controller", link: "/zh/api/controller" },
  { text: "文档与编辑", link: "/zh/api/document-edit" },
  { text: "光标、选区、滚动", link: "/zh/api/caret-selection" },
  { text: "外观", link: "/zh/api/appearance" },
  { text: "事件", link: "/zh/api/events" },
  { text: "Span", link: "/zh/api/decorations-spans" },
  { text: "补全 API", link: "/zh/api/completion" },
  { text: "搜索", link: "/zh/api/search" },
  { text: "折叠", link: "/zh/api/folding" },
  { text: "语言与括号", link: "/zh/api/language" },
  { text: "键位", link: "/zh/api/keymap" },
  { text: "叠加装饰", link: "/zh/api/overlays" },
  { text: "参考线", link: "/zh/api/guides" },
  { text: "Diff", link: "/zh/api/diff" },
  { text: "联动编辑", link: "/zh/api/linked-editing" },
  { text: "换行动作", link: "/zh/api/newline" },
];

export default defineConfig({
  title: "SweetEditor Compose",
  description: "Code editor for Compose Multiplatform",
  base: "/SweetEditorCompose/",
  lastUpdated: true,
  ignoreDeadLinks: true,
  themeConfig: {
    socialLinks: [
      { icon: "github", link: "https://github.com/lumkit/SweetEditorCompose" },
    ],
    search: { provider: "local" },
  },
  locales: {
    root: {
      label: "English",
      lang: "en",
      description: "Native C++ editor core for Compose Multiplatform",
      themeConfig: {
        nav: [
          { text: "Guide", link: "/guide/install" },
          { text: "API", link: "/api/sweet-editor" },
          { text: "GitHub", link: "https://github.com/lumkit/SweetEditorCompose" },
        ],
        sidebar: [
          { text: "Guide", items: guideEn },
          { text: "API", items: apiEn },
        ],
        outline: { level: [2, 3] },
      },
    },
    zh: {
      label: "简体中文",
      lang: "zh-CN",
      description: "面向 Compose Multiplatform 的原生 C++ 代码编辑器",
      themeConfig: {
        nav: [
          { text: "指南", link: "/zh/guide/install" },
          { text: "API", link: "/zh/api/sweet-editor" },
          { text: "GitHub", link: "https://github.com/lumkit/SweetEditorCompose" },
        ],
        sidebar: [
          { text: "指南", items: guideZh },
          { text: "API", items: apiZh },
        ],
        outline: { level: [2, 3] },
        outlineTitle: "本页目录",
        lastUpdatedText: "最后更新",
        docFooter: { prev: "上一页", next: "下一页" },
      },
    },
  },
});
