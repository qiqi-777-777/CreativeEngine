import sys
import re
from pathlib import Path

def split_items(text):
    if not text:
        return []
    # 统一中文逗号为顿号
    s = text.replace('，', '、').replace(';', '、').replace('；', '、')
    parts = [p.strip() for p in s.split('、') if p.strip()]
    items = []
    for p in parts:
        # 再按斜杠细分，如 Python/R/SQL
        subparts = [sp.strip() for sp in re.split(r'[\/]', p) if sp.strip()]
        items.extend(subparts)
    # 去重保持顺序
    seen = set()
    unique = []
    for x in items:
        if x not in seen:
            unique.append(x)
            seen.add(x)
    return unique

def sql_escape(val: str) -> str:
    return val.replace("\\", "\\\\").replace("'", "\\'")

def parse_markdown_table(md_path: Path):
    rows = []
    with md_path.open('r', encoding='utf-8') as f:
        lines = f.readlines()

    for line in lines:
        line = line.strip()
        # 只处理以 | 开头的表格行
        if not line.startswith('|'):
            continue
        # 跳过分隔行
        if set(line.replace('|', '').strip()) <= set('- '):
            continue

        # 去掉首尾的 | 后按 | 分列
        cells = [c.strip() for c in line.split('|')[1:-1]]
        if len(cells) != 7:
            # header 行可能也会进来，过滤掉非数据行
            # 简单判断：第一列必须是数字序号
            if not (cells and cells[0].isdigit()):
                continue

        # cells: [序号, 职业名称, 匹配专业, 技术要求, 行业热度, 适合性格, 推荐理由]
        idx_str, name, majors_str, skills_str, heat, personality, reason = cells
        if not idx_str.isdigit():
            continue
        idx = int(idx_str)
        majors = split_items(majors_str)
        skills = split_items(skills_str)
        rows.append({
            'id': idx,
            'name': name,
            'majors': majors,
            'skills': skills,
            'heat': heat,
            'personality': personality,
            'reason': reason,
        })
    return rows

def build_dimensions(rows):
    major_set = []
    skill_set = []
    seen_major = set()
    seen_skill = set()
    for r in rows:
        for m in r['majors']:
            if m and m not in seen_major:
                seen_major.add(m)
                major_set.append(m)
        for s in r['skills']:
            if s and s not in seen_skill:
                seen_skill.add(s)
                skill_set.append(s)
    # 分配 ID
    major_id_map = {name: i + 1 for i, name in enumerate(major_set)}
    skill_id_map = {name: i + 1 for i, name in enumerate(skill_set)}
    return major_set, skill_set, major_id_map, skill_id_map

def generate_sql(rows, major_set, skill_set, major_id_map, skill_id_map):
    out = []
    out.append("SET NAMES utf8mb4;")
    out.append("-- 可选：USE creative_engine;")
    out.append("START TRANSACTION;")

    # 维表插入（显式指定 id，便于关联）
    for name in major_set:
        out.append(f"INSERT INTO major (id, name) VALUES ({major_id_map[name]}, '{sql_escape(name)}') ON DUPLICATE KEY UPDATE name=VALUES(name);")
    for name in skill_set:
        out.append(f"INSERT INTO skill (id, name) VALUES ({skill_id_map[name]}, '{sql_escape(name)}') ON DUPLICATE KEY UPDATE name=VALUES(name);")

    # 职业主表
    for r in rows:
        out.append(
            "INSERT INTO occupation (id, name, industry_heat, personality_code, reason) "
            f"VALUES ({r['id']}, '{sql_escape(r['name'])}', '{sql_escape(r['heat'])}', '{sql_escape(r['personality'])}', '{sql_escape(r['reason'])}') "
            "ON DUPLICATE KEY UPDATE name=VALUES(name), industry_heat=VALUES(industry_heat), personality_code=VALUES(personality_code), reason=VALUES(reason);"
        )

    # 关联表插入（去重）
    for r in rows:
        occ_id = r['id']
        for m in r['majors']:
            mid = major_id_map.get(m)
            if mid:
                out.append(
                    f"INSERT IGNORE INTO occupation_major (occupation_id, major_id) VALUES ({occ_id}, {mid});"
                )
        for s in r['skills']:
            sid = skill_id_map.get(s)
            if sid:
                out.append(
                    f"INSERT IGNORE INTO occupation_skill (occupation_id, skill_id) VALUES ({occ_id}, {sid});"
                )

    out.append("COMMIT;")
    return "\n".join(out)

def main():
    if len(sys.argv) < 3:
        print("Usage: python md_occupation_to_sql.py <input_md_path> <output_sql_path>")
        sys.exit(1)

    md_path = Path(sys.argv[1])
    out_path = Path(sys.argv[2])

    rows = parse_markdown_table(md_path)
    if not rows:
        print("No data parsed from markdown table. Check file format.")
        sys.exit(2)

    major_set, skill_set, major_id_map, skill_id_map = build_dimensions(rows)
    sql_text = generate_sql(rows, major_set, skill_set, major_id_map, skill_id_map)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(sql_text, encoding='utf-8')
    print(f"Generated SQL: {out_path}")

if __name__ == "__main__":
    main()