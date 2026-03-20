/**
 * 搜索条件项
 * 参照 apps/web-antd/src/api/requestParam.json 结构
 */
export interface SearchItem {
  /** 字段名 */
  field?: string;
  /** 操作符: eq(等于), ne(不等于), gt(大于), ge(大于等于), lt(小于), le(小于等于), like(包含), notlike(不包含), in(包含于), notin(不包含于), isnull(为空), notnull(不为空) */
  op?: string;
  /** 字段值 */
  val?: any;
  /** 组内逻辑 (AND / OR)，仅当 children 不为空时有效 */
  logic?: 'AND' | 'OR';
  /** 子条件列表，用于构建嵌套查询 */
  children?: SearchItem[];
}

/**
 * 排序项
 */
export interface OrderItem {
  /** 排序字段名 */
  column: string;
  /** 是否升序 */
  asc: boolean;
}

/**
 * 搜索请求参数
 * 参照 apps/web-antd/src/api/requestParam.json 结构
 */
export interface SearchRequest {
  /** 当前页码 */
  pageNumber: number;
  /** 每页大小 */
  pageSize: number;
  /** 搜索关键词（简单搜索时使用） */
  keyword?: string;
  /** 搜索字段列表（简单搜索时使用） */
  searchFields?: string[];
  /** items 条件之间的逻辑关系 (AND / OR) */
  logic?: 'AND' | 'OR';
  /** 高级搜索条件列表 */
  items?: SearchItem[];
  /** 多字段排序列表 */
  orders?: OrderItem[];
}

/**
 * 构建搜索条件的快捷方法
 * @param field 字段名
 * @param op 操作符
 * @param val 字段值
 * @param logic 组内逻辑（可选）
 */
export function buildSearchItem(
  field: string,
  op: string,
  val: any,
  logic?: 'AND' | 'OR',
): SearchItem {
  return {
    field,
    op,
    val,
    logic,
  };
}

/**
 * 构建组合搜索条件
 * @param logic 组合逻辑
 * @param children 子条件
 */
export function buildSearchGroup(
  logic: 'AND' | 'OR',
  children: SearchItem[],
): SearchItem {
  return {
    logic,
    children,
  };
}
