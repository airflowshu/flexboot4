import type { CmsFileObject } from './types';

import { requestClient } from '#/api/request';

export interface CmsUploadParams {
  bizId?: string;
  tenantId?: string;
}

export async function uploadCmsFileApi(file: File, params?: CmsUploadParams) {
  return requestClient.upload<CmsFileObject>(
    '/admin/cms/file/upload',
    { file },
    {
      params,
      headers: {
        'Content-Type': undefined,
      },
    },
  );
}
