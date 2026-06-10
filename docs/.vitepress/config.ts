export default {
  base: '/flexboot4/',
  description: 'FlexBoot4 前后端分离开发框架文档',
  lang: 'zh-CN',
  title: 'FlexBoot4',
  themeConfig: {
    docFooter: {
      next: '下一页',
      prev: '上一页',
    },
    editLink: {
      pattern: 'https://github.com/airflowshu/flexboot4/edit/master/docs/:path',
      text: '在 GitHub 上编辑此页',
    },
    footer: {
      copyright: 'Copyright © 2026 FlexBoot4',
      message: 'Released under the open source license.',
    },
    lastUpdated: {
      formatOptions: {
        dateStyle: 'medium',
        timeStyle: 'short',
      },
      text: '最后更新',
    },
    nav: [
      { text: '首页', link: '/' },
      { text: '快速开始', link: '/QUICKSTART' },
      { text: '开发指南', link: '/guide' },
      { text: '模块', link: '/modules' },
      {
        text: '专题',
        items: [
          { text: '数据库迁移与 Flyway', link: '/database-migration' },
          { text: 'Admin 认证与账号安全', link: '/admin-auth-security' },
          { text: '第三方登录集成', link: '/admin-social-login' },
          { text: '权限设计', link: '/backend_permission_control_design' },
          { text: 'OpenAPI 标签分组', link: '/API_TAG_GROUP_GUIDE' },
          { text: 'CRUD 生成契约', link: '/crud_module_generator_contract' },
          { text: 'AI 厂商模型接入', link: '/ai-provider-models' },
          { text: '常见问题', link: '/FAQ' },
        ],
      },
    ],
    outline: {
      label: '本页目录',
      level: [2, 3],
    },
    search: {
      provider: 'local',
      options: {
        translations: {
          button: {
            buttonAriaLabel: '搜索文档',
            buttonText: '搜索',
          },
          modal: {
            displayDetails: '显示详细列表',
            footer: {
              closeKeyAriaLabel: 'Esc',
              closeText: '关闭',
              navigateDownKeyAriaLabel: '向下',
              navigateText: '切换',
              navigateUpKeyAriaLabel: '向上',
              selectKeyAriaLabel: 'Enter',
              selectText: '选择',
            },
            noResultsText: '没有找到相关结果',
            resetButtonTitle: '清除搜索',
          },
        },
      },
    },
    sidebar: [
      {
        text: '开始',
        items: [
          { text: '文档中心', link: '/' },
          { text: '快速开始', link: '/QUICKSTART' },
          { text: '常见问题', link: '/FAQ' },
        ],
      },
      {
        text: '开发指南',
        items: [
          { text: '开发者指南', link: '/guide' },
          { text: 'Starter 架构', link: '/STARTER_ARCHITECTURE' },
          { text: '模块文档索引', link: '/modules' },
          { text: 'AI 厂商模型接入', link: '/ai-provider-models' },
          { text: '通用查询', link: '/Mf基础功能' },
        ],
      },
      {
        text: '核心专题',
        items: [
          { text: '数据库迁移与 Flyway', link: '/database-migration' },
          { text: 'Admin 认证与账号安全', link: '/admin-auth-security' },
          { text: '第三方登录集成', link: '/admin-social-login' },
          { text: '后端权限控制设计', link: '/backend_permission_control_design' },
          { text: 'OpenAPI 标签分组', link: '/API_TAG_GROUP_GUIDE' },
          { text: 'CRUD 生成契约', link: '/crud_module_generator_contract' },
        ],
      },
    ],
    sidebarMenuLabel: '菜单',
    socialLinks: [
      { icon: 'github', link: 'https://github.com/airflowshu/flexboot4' },
    ],
  },
};
