import re
import json

with open(r'C:\Users\karth\.gemini\antigravity\scratch\logancee-frontend\src\mockData\index.ts', 'r', encoding='utf-8') as f:
    ts_code = f.read()

# Let's extract items array strings using regex or python parsing
def parse_items_block(block_name):
    pattern = rf'const {block_name} = \[\s*(.*?)\s*\];'
    match = re.search(pattern, ts_code, re.DOTALL)
    if not match:
        return []
    block = match.group(1)
    # Parse individual objects inside block
    # Object format: { name: '...', sub: '...', price: ..., orig: ..., brand: '...', img1: '...', img2: '...', ... }
    obj_pattern = r'\{([^}]+)\}'
    objs = re.findall(obj_pattern, block)
    parsed = []
    for obj_str in objs:
        item = {}
        # key: val
        pairs = re.findall(r'(\w+):\s*(\'.*?\'|".*?"|\d+(?:\.\d+)?|true|false)', obj_str)
        for k, v in pairs:
            if v.startswith("'") or v.startswith('"'):
                item[k] = v[1:-1]
            elif v == 'true':
                item[k] = True
            elif v == 'false':
                item[k] = False
            else:
                item[k] = float(v) if '.' in v else int(v)
        parsed.append(item)
    return parsed

women_items = parse_items_block('womenItems')
men_items = parse_items_block('menItems')
kids_items = parse_items_block('kidsItems')
beauty_items = parse_items_block('beautyItems')
electronics_items = parse_items_block('electronicsItems')
home_items = parse_items_block('homeItems')

print(f"Parsed: women={len(women_items)}, men={len(men_items)}, kids={len(kids_items)}, beauty={len(beauty_items)}, elec={len(electronics_items)}, home={len(home_items)}")
