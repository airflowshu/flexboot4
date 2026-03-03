import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "FlexBoot4",
  base: '/flexboot4/',
  description: "现代化企业级 AI 中台与后台管理聚合系统 | Spring Boot 4 + MyBatis-Flex + AI Gateway",
  ignoreDeadLinks: true,
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: '首页', link: '/' },
      { text: '快速开始', link: '/guide' },
      {
        text: '文档',
        items: [
          { text: 'Starter 架构', link: '/STARTER_ARCHITECTURE' },
          { text: 'SMS4J 模块', link: '/SMS4J_STARTER' },
          { text: '权限控制', link: '/backend_permission_control_design' },
          { text: '用户注销方案', link: '/../plan-userDeaccount.prompt' },
          { text: '快速参考', link: '/QUICKSTART' }
        ]
      },
      {
        text: '相关资源',
        items: [
          { text: 'GitHub', link: 'https://github.com/yunlbd/flexboot4' },
          { text: 'FAQ', link: '/FAQ' }
        ]
      }
    ],

    sidebar: [
      {
        text: '快速开始',
        items: [
          { text: '接入指南', link: '/guide' }
        ]
      },
      {
        text: '核心文档',
        items: [
          { text: 'Starter 架构设计', link: '/STARTER_ARCHITECTURE' },
          { text: 'SMS4J Starter', link: '/SMS4J_STARTER' },
          { text: '权限控制实现', link: '/backend_permission_control_design' },
          { text: '用户注销完整方案', link: '/../plan-userDeaccount.prompt' },
          { text: '快速开始指南', link: '/QUICKSTART' }
        ]
      },
      {
        text: '参考',
        items: [
          { text: '常见问题', link: '/FAQ' },
          { text: 'API 标签分组', link: '/API_TAG_GROUP_GUIDE' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/yunlbd/flexboot4' }
    ],

    footer: {
      message: 'Apache License 2.0',
      copyright: 'Copyright © 2024-present FlexBoot4'
    }
  }
})
