"""验证 JSON 后仅移除字符串外的空白，保留数字词法、字符串与键顺序。作者：mryt233"""
import json
from decimal import Decimal


def compact_json(data: bytes) -> bytes:
    text = data.decode("utf-8")
    def invalid(value):
        raise ValueError("JSON 含非标准数字：" + value)
    json.loads(text, parse_float=Decimal, parse_int=Decimal, parse_constant=invalid)
    quoted = escaped = False
    result = []
    for char in text:
        if quoted:
            result.append(char)
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
        elif char == '"':
            quoted = True
            result.append(char)
        elif char not in " \t\r\n":
            result.append(char)
    return "".join(result).encode("utf-8")
