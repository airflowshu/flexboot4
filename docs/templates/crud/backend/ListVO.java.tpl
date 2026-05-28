package {{packageBase}}.vo.{{domain}};

import {{packageBase}}.vo.sys.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class {{entity}}ListVO extends BaseAuditVO {
{{listVoFields}}
}
