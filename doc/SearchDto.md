交互体验 (UX) ：

- 简单场景 ：大多数列表页只有一个简单的“搜索框”。前端直接传 keyword 比构造复杂的嵌套对象要简单得多，开发效率更高。
- 高级场景 ：只有在点击“高级筛选”弹窗时，才需要用到 items 的递归结构。
- 这样既保留了简单场景的便捷性，又提供了复杂场景的强大能力。后端逻辑中，它们是 AND 并集的关系（即：必须满足 Keyword 搜索 并且 满足高级筛选条件）。

```json
{
  "pageNumber": 1,
  "pageSize": 10,
  "keyword": "test",
  // 快捷搜索：所有 searchFields 字段都会匹配
  "searchFields": [
    "name", "code"
  ],
  "logic": "AND",
  "items": [
    // 高级搜索：与 keyword 搜索是 AND 关系
    {
      "field": "status",
      "op": "eq",
      "val": 1
    },
    {
      "logic": "OR",
      // 嵌套逻辑演示
      "children": [
        {
          "field": "type",
          "op": "eq",
          "val": "A"
        },
        {
          "field": "type",
          "op": "eq",
          "val": "B"
        }
      ]
    }
  ]
  // ...
}
```