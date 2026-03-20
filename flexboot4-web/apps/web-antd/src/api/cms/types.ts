export interface CmsPageResult<T> {
  pageNumber: number;
  pageSize: number;
  totalRow: number;
  records: T[];
}

export interface CmsFileObject {
  id?: string;
  fileName?: string;
  fileExt?: string;
  fileSize?: number;
  location?: {
    bucket?: string;
    endpoint?: string;
    objectKey?: string;
    region?: string;
    storageType?: 'ALI_OSS' | 'LOCAL' | 'MINIO' | 'S3';
  };
}
