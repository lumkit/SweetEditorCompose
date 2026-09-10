#!/usr/bin/env python3
"""Generate Kotlin CoreProtocol types from SE schema.snapshot.json."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

VECTOR_RE = re.compile(r"^(?:Vector|std::vector)<(.+)>$")
KOTLIN_KEYWORDS = {
    "in", "out", "fun", "val", "var", "object", "class", "interface", "is",
    "as", "type", "when", "else", "this", "super", "package", "import",
}


def snake_to_camel(name: str) -> str:
    parts = name.split("_")
    raw = parts[0] + "".join(p[:1].upper() + p[1:] for p in parts[1:] if p)
    return f"`{raw}`" if raw in KOTLIN_KEYWORDS else raw


def vector_inner(cpp_type: str) -> str | None:
    match = VECTOR_RE.match(" ".join(cpp_type.replace("const ", "").split()))
    return match.group(1).strip() if match else None


def is_hidden(item: dict) -> bool:
    return item["direction"] == "in" and (
        item["kind"] == "payload" or item["name"].endswith("Entry")
    )


def enum_const(name: str) -> str:
    if name[0].isdigit():
        return f"`{name}`"
    return name


def kt_type(field: dict, enums: dict, types: dict) -> str:
    inner = vector_inner(field["cpp_type"])
    if field.get("map_entry"):
        entry = types.get(inner or "")
        if inner and inner in types and len(types[inner]["fields"]) == 2:
            k = kt_type(types[inner]["fields"][0], enums, types)
            v = kt_type(types[inner]["fields"][1], enums, types)
            return f"Map<{k}, {v}>"
        if entry is None:
            # Vector<pair> synthetic - use inner name
            pass
    if inner:
        inner_field = {
            "cpp_type": inner,
            "wire": field["wire"] if False else None,
            "name": "item",
        }
        # reconstruct wire for inner
        dummy = {"cpp_type": inner, "wire": infer_inner_wire(inner, field, enums, types), "name": "e"}
        return f"List<{kt_scalar(dummy, enums, types)}>"
    return kt_scalar(field, enums, types)


def infer_inner_wire(inner: str, parent: dict, enums: dict, types: dict) -> str:
    if inner in enums:
        return "enum_i32"
    if inner in types:
        return "struct"
    mapping = {
        "uint8_t": "u8",
        "uint16_t": "u16",
        "float": "f32",
        "double": "f64",
        "int32_t": "i32",
        "uint32_t": "u32",
        "int64_t": "i64",
        "uint64_t": "u64",
        "bool": "bool_i32",
        "U8String": "utf8_string",
        "U16String": "u16_string",
        "PointF": "struct",
    }
    return mapping.get(inner, parent.get("wire") or "struct")


def kt_scalar(field: dict, enums: dict, types: dict) -> str:
    cpp = field["cpp_type"]
    wire = field["wire"]
    if wire == "enum_i32" and cpp in enums:
        return cpp
    if wire in ("utf8_string", "u16_string", "u16_as_utf8"):
        return "String"
    if wire in ("bool_i32", "bool_u8"):
        return "Boolean"
    if wire in ("f32",):
        return "Float"
    if wire in ("f64",):
        return "Double"
    if wire in ("i64", "u64", "size_as_i64", "size_as_u64"):
        return "Long"
    if wire in ("u8", "u16", "i32", "u32", "size_as_i32", "size_as_u32", "enum_i32"):
        return "Int"
    if cpp in types:
        return cpp
    if cpp in enums:
        return cpp
    raise SystemExit(f"Unsupported type {cpp} wire={wire}")


def boxed(name: str) -> str:
    return {
        "Int": "Int",
        "Long": "Long",
        "Float": "Float",
        "Double": "Double",
        "Boolean": "Boolean",
    }.get(name, name)


def read_expr(field: dict, enums: dict, types: dict) -> str:
    inner = vector_inner(field["cpp_type"])
    if field.get("map_entry") and inner:
        entry_name = inner
        return f"readMap{entry_name}(reader)"
    if inner:
        safe = inner.replace("<", "").replace(">", "").replace("::", "")
        return f"readList{safe}(reader)"
    wire = field["wire"]
    cpp = field["cpp_type"]
    if wire == "enum_i32" and cpp in enums:
        return f"{cpp}.fromValue(reader.readI32())"
    return {
        "u8": "reader.readU8()",
        "u16": "reader.readU16()",
        "i32": "reader.readI32()",
        "u32": "reader.readU32()",
        "size_as_i32": "reader.readI32()",
        "size_as_u32": "reader.readU32()",
        "enum_i32": "reader.readI32()",
        "i64": "reader.readI64()",
        "u64": "reader.readU64()",
        "size_as_i64": "reader.readI64()",
        "size_as_u64": "reader.readU64()",
        "f32": "reader.readF32()",
        "f64": "reader.readF64()",
        "bool_i32": "reader.readBoolI32()",
        "bool_u8": "reader.readBoolU8()",
        "utf8_string": "reader.readUtf8String()",
        "u16_string": "reader.readUtf8String()",
        "u16_as_utf8": "reader.readUtf8String()",
        "struct": f"read{cpp}(reader)",
    }.get(wire, f"read{cpp}(reader)")


def write_stmt(field: dict, expr: str, enums: dict, types: dict) -> str:
    inner = vector_inner(field["cpp_type"])
    if field.get("map_entry") and inner:
        return f"writeMap{inner}(writer, {expr})"
    if inner:
        safe = inner.replace("<", "").replace(">", "")
        return f"writeList{safe}(writer, {expr})"
    wire = field["wire"]
    cpp = field["cpp_type"]
    if wire == "enum_i32" and cpp in enums:
        return f"writer.writeI32({expr}.value)"
    mapping = {
        "u8": f"writer.writeU8({expr})",
        "u16": f"writer.writeU16({expr})",
        "i32": f"writer.writeI32({expr})",
        "u32": f"writer.writeU32({expr})",
        "size_as_i32": f"writer.writeI32({expr})",
        "size_as_u32": f"writer.writeU32({expr})",
        "enum_i32": f"writer.writeI32({expr})",
        "i64": f"writer.writeI64({expr})",
        "u64": f"writer.writeU64({expr})",
        "size_as_i64": f"writer.writeI64({expr})",
        "size_as_u64": f"writer.writeU64({expr})",
        "f32": f"writer.writeF32({expr})",
        "f64": f"writer.writeF64({expr})",
        "bool_i32": f"writer.writeBoolI32({expr})",
        "bool_u8": f"writer.writeBoolU8({expr})",
        "utf8_string": f"writer.writeUtf8String({expr})",
        "u16_string": f"writer.writeUtf8String({expr})",
        "u16_as_utf8": f"writer.writeUtf8String({expr})",
        "struct": f"write{cpp}(writer, {expr})",
    }
    return mapping.get(wire, f"write{cpp}(writer, {expr})")


def gen_enums(enums: list) -> str:
    parts = []
    for item in enums:
        name = item["name"]
        if item["kind"] in ("flags", "consts"):
            lines = [f"internal object {name} {{"]
            for v in item["values"]:
                lines.append(f"    const val {enum_const(v['name'])}: Int = {v['value']}")
            lines.append("}")
            parts.append("\n".join(lines))
            continue
        lines = [f"internal enum class {name}(val value: Int) {{"]
        members = []
        for v in item["values"]:
            members.append(f"    {enum_const(v['name'])}({v['value']})")
        lines.append(",\n".join(members))
        lines.append("    ;")
        lines.append("    companion object {")
        lines.append(f"        fun fromValue(value: Int): {name} =")
        lines.append("            entries.find { it.value == value }")
        lines.append(f"                ?: throw IllegalArgumentException(\"Unknown {name}: $value\")")
        lines.append("    }")
        lines.append("}")
        parts.append("\n".join(lines))
    return "\n\n".join(parts)


def gen_data_class(item: dict, enums: dict, types: dict) -> str:
    name = item["name"]
    fields = []
    for field in item["fields"]:
        fname = snake_to_camel(field["name"])
        ftype = kt_type(field, enums, types)
        fields.append(f"    val {fname}: {ftype}")
    body = ",\n".join(fields) if fields else ""
    if not fields:
        return f"internal data class {name}(val unused: Int = 0)"
    return f"internal data class {name}(\n{body},\n)"


def collect_list_inners(schema: dict) -> set[str]:
    types = {t["name"]: t for t in schema["types"]}
    hidden = {t["name"] for t in schema["types"] if is_hidden(t)}
    names = set()
    for item in schema["types"]:
        for field in item["fields"]:
            inner = vector_inner(field["cpp_type"])
            if inner and not field.get("map_entry") and inner not in hidden:
                names.add(inner)
    return names


def collect_maps(schema: dict) -> list[tuple[str, dict, dict]]:
    types = {t["name"]: t for t in schema["types"]}
    result = []
    seen = set()
    for item in schema["types"]:
        for field in item["fields"]:
            inner = vector_inner(field["cpp_type"])
            if not field.get("map_entry") or not inner:
                continue
            if inner in seen:
                continue
            seen.add(inner)
            entry = types.get(inner)
            if entry is None or len(entry["fields"]) != 2:
                continue
            result.append((inner, entry["fields"][0], entry["fields"][1]))
    return result


def gen_codec(schema: dict) -> str:
    enums = {e["name"]: e for e in schema["enums"]}
    types = {t["name"]: t for t in schema["types"]}
    lines = [
        "internal object CoreProtocol {",
    ]
    for item in schema["types"]:
        if is_hidden(item) and item["kind"] == "payload":
            continue
        name = item["name"]
        needs_read = True
        needs_write = True
        if needs_read:
            if item["direction"] in ("out", "both", "value"):
                lines.append(f"    fun decode{name}(bytes: ByteArray): {name} = read{name}(ProtocolReader(bytes))")
            lines.append(f"    private fun read{name}(reader: ProtocolReader): {name} {{")
            args = []
            for field in item["fields"]:
                fname = snake_to_camel(field["name"])
                lines.append(f"        val {fname} = {read_expr(field, enums, types)}")
                args.append(fname)
            if item["fields"]:
                lines.append(f"        return {name}({', '.join(args)})")
            else:
                lines.append(f"        return {name}()")
            lines.append("    }")
        if needs_write:
            if item["direction"] in ("in", "both", "value"):
                lines.append(f"    fun encode{name}(value: {name}): ByteArray {{")
                lines.append("        val writer = ProtocolWriter()")
                lines.append(f"        write{name}(writer, value)")
                lines.append("        return writer.toByteArray()")
                lines.append("    }")
            lines.append(f"    private fun write{name}(writer: ProtocolWriter, value: {name}) {{")
            for field in item["fields"]:
                fname = snake_to_camel(field["name"])
                lines.append(f"        {write_stmt(field, 'value.' + fname, enums, types)}")
            lines.append("    }")

    for inner in sorted(collect_list_inners(schema)):
        dummy = {"cpp_type": inner, "wire": infer_inner_wire(inner, {"wire": "struct"}, enums, types), "name": "item"}
        elem = kt_scalar(dummy, enums, types) if inner not in types else inner
        if inner in types:
            dummy["wire"] = "struct"
            dummy["cpp_type"] = inner
        lines.append(f"    private fun readList{inner}(reader: ProtocolReader): List<{elem}> {{")
        lines.append("        val count = reader.readListCount()")
        lines.append("        return List(count) {")
        if inner in types:
            lines.append(f"            read{inner}(reader)")
        elif inner in enums:
            lines.append(f"            {inner}.fromValue(reader.readI32())")
        else:
            dummy_field = {"cpp_type": inner, "wire": infer_inner_wire(inner, {"wire": "struct"}, enums, types), "name": "x"}
            lines.append(f"            {read_expr(dummy_field, enums, types)}")
        lines.append("        }")
        lines.append("    }")
        lines.append(f"    private fun writeList{inner}(writer: ProtocolWriter, values: List<{elem}>) {{")
        lines.append("        writer.writeI32(values.size)")
        lines.append("        for (item in values) {")
        if inner in types:
            lines.append(f"            write{inner}(writer, item)")
        elif inner in enums:
            lines.append("            writer.writeI32(item.value)")
        else:
            dummy_field = {"cpp_type": inner, "wire": infer_inner_wire(inner, {"wire": "struct"}, enums, types), "name": "x"}
            lines.append(f"            {write_stmt(dummy_field, 'item', enums, types)}")
        lines.append("        }")
        lines.append("    }")

    for inner, key_f, val_f in collect_maps(schema):
        ktype = kt_scalar(key_f, enums, types)
        vtype = kt_type(val_f, enums, types)
        lines.append(f"    private fun readMap{inner}(reader: ProtocolReader): Map<{ktype}, {vtype}> {{")
        lines.append("        val count = reader.readI32()")
        lines.append("        val map = LinkedHashMap<{ktype}, {vtype}>(count)".replace("{ktype}", ktype).replace("{vtype}", vtype))
        lines.append("        repeat(count) {")
        lines.append(f"            val key = {read_expr(key_f, enums, types)}")
        lines.append(f"            val value = {read_expr(val_f, enums, types)}")
        lines.append("            map[key] = value")
        lines.append("        }")
        lines.append("        return map")
        lines.append("    }")
        lines.append(f"    private fun writeMap{inner}(writer: ProtocolWriter, map: Map<{ktype}, {vtype}>) {{")
        lines.append("        writer.writeI32(map.size)")
        lines.append("        for ((key, value) in map) {")
        lines.append(f"            {write_stmt(key_f, 'key', enums, types)}")
        lines.append(f"            {write_stmt(val_f, 'value', enums, types)}")
        lines.append("        }")
        lines.append("    }")

    lines.append("}")
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--schema", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    schema = json.loads(Path(args.schema).read_text(encoding="utf-8"))
    if schema.get("format") != "sweeteditor.protocol.schema":
        raise SystemExit("Unexpected schema format")
    enums = {e["name"]: e for e in schema["enums"]}
    types = {t["name"]: t for t in schema["types"]}
    visible = [t for t in schema["types"] if not is_hidden(t)]
    models = "\n\n".join(gen_data_class(t, enums, types) for t in visible)
    text = (
        "// GENERATED by tools/kotlin_protocol_gen/generate.py. Do not edit.\n"
        "package io.github.lumkit.sweeteditor.core.protocol\n\n"
        + gen_enums(schema["enums"])
        + "\n\n"
        + models
        + "\n\n"
        + gen_codec(schema)
        + "\n"
    )
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(text, encoding="utf-8")
    print(f"Wrote {out}")


if __name__ == "__main__":
    main()
