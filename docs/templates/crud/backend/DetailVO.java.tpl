package {{packageBase}}.vo.{{domain}};

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class {{entity}}DetailVO extends {{entity}}ListVO {
{{detailVoFields}}
}
