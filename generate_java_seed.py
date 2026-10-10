import re

with open(r'C:\Users\karth\.gemini\antigravity\scratch\logancee-frontend\src\mockData\index.ts', 'r', encoding='utf-8') as f:
    ts_code = f.read()

def parse_items(block_name):
    pattern = rf'const {block_name} = \[\s*(.*?)\s*\];'
    match = re.search(pattern, ts_code, re.DOTALL)
    if not match:
        return []
    block = match.group(1)
    objs = re.findall(r'\{([^}]+)\}', block)
    parsed = []
    for obj_str in objs:
        item = {}
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

def escape_java(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')

categories_code = """
    public static List<Category> getCategories() {
        List<Category> list = new ArrayList<>();
        list.add(Category.builder().id("cat-women").name("Women").slug("women")
                .image("https://images.unsplash.com/photo-1490481651871-ab68de25d43d?auto=format&fit=crop&w=1000&q=80")
                .description("Haute couture, contemporary dresses, tailored suits & luxury accessories.")
                .itemCount(1240).subcategories(Arrays.asList("Dresses", "Tops", "Jeans", "Ethnic Wear", "Handbags", "Shoes", "Jewellery", "Accessories")).build());
        list.add(Category.builder().id("cat-men").name("Men").slug("men")
                .image("https://images.unsplash.com/photo-1617137984095-74e4e5e3613f?auto=format&fit=crop&w=1000&q=80")
                .description("Refined menswear, bespoke tailoring, premium leather shoes & watches.")
                .itemCount(980).subcategories(Arrays.asList("Shirts", "T-Shirts", "Jeans", "Trousers", "Jackets", "Shoes", "Watches", "Accessories")).build());
        list.add(Category.builder().id("cat-kids").name("Kids").slug("kids")
                .image("https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?auto=format&fit=crop&w=1000&q=80")
                .description("Playful yet sophisticated apparel & accessories for young trendsetters.")
                .itemCount(540).subcategories(Arrays.asList("Boys Fashion", "Girls Fashion", "Footwear", "Toys & Accessories")).build());
        list.add(Category.builder().id("cat-beauty").name("Beauty & Self Care").slug("beauty")
                .image("https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?auto=format&fit=crop&w=1000&q=80")
                .description("Clean skincare formulas, botanical hair treats & luxury fragrances.")
                .itemCount(760).subcategories(Arrays.asList("Skincare", "Haircare", "Body Care", "Personal Care", "Grooming")).build());
        list.add(Category.builder().id("cat-electronics").name("Electronics").slug("electronics")
                .image("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80")
                .description("Next-gen audio gear, sleek smartwatches & minimalist lifestyle tech.")
                .itemCount(420).subcategories(Arrays.asList("Mobiles", "Laptops", "Headphones", "Smart Watches", "Earbuds", "Accessories")).build());
        list.add(Category.builder().id("cat-home").name("Home & Lifestyle").slug("home")
                .image("https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=1000&q=80")
                .description("Artisanal home decor, luxury bedding, statement lighting & travel bags.")
                .itemCount(680).subcategories(Arrays.asList("Home Decor", "Furniture", "Lighting", "Kitchen", "Watches", "Bags", "Travel Accessories")).build());
        return list;
    }
"""

brands_code = """
    public static List<Brand> getBrands() {
        List<Brand> list = new ArrayList<>();
        list.add(Brand.builder().id("brand-1").name("ATELIER LOGANCEE").logo("").category("Women").build());
        list.add(Brand.builder().id("brand-2").name("MAISON NOIR").logo("").category("Men").build());
        list.add(Brand.builder().id("brand-3").name("AURORA SKIN").logo("").category("Beauty").build());
        list.add(Brand.builder().id("brand-4").name("VALENTI ITALY").logo("").category("Women").build());
        list.add(Brand.builder().id("brand-5").name("NEO AUDIO").logo("").category("Electronics").build());
        list.add(Brand.builder().id("brand-6").name("SARTORIAL CO.").logo("").category("Men").build());
        list.add(Brand.builder().id("brand-7").name("LUMINA LIVING").logo("").category("Home").build());
        list.add(Brand.builder().id("brand-8").name("HERITAGE DENIM").logo("").category("Men").build());
        list.add(Brand.builder().id("brand-9").name("VERVE COSMETICS").logo("").category("Beauty").build());
        list.add(Brand.builder().id("brand-10").name("CHRONO SWISS").logo("").category("Electronics").build());
        return list;
    }
"""

def generate_product_code(items, cat_name, prefix, desc, specs, colors_java, sizes_java):
    code_lines = []
    for idx, item in enumerate(items):
        pid = f"{prefix}-{100 + idx}" if prefix in ['wom', 'men', 'kid'] else f"{prefix}-{idx}"
        sku = f"LOG-{prefix.upper()}-{100 + idx}"
        name = escape_java(item.get('name', 'Product'))
        brand = escape_java(item.get('brand', 'LOGANCEE'))
        sub = escape_java(item.get('sub', 'General'))
        price = float(item.get('price', 1000))
        orig = float(item.get('orig', price * 1.3))
        disc = int(round(((orig - price) / orig) * 100))
        rating = float(item.get('rating', 4.8))
        rev = int(item.get('rev', 50))
        img1 = escape_java(item.get('img1', 'https://images.unsplash.com/photo-1584273143981-41c073dfe8f8?auto=format&fit=crop&w=1000&q=80'))
        img2 = escape_java(item.get('img2', img1))
        is_new = 'true' if item.get('isNew', False) else 'false'
        is_trend = 'true' if item.get('isTrending', False) else 'false'
        is_best = 'true' if item.get('isBest', False) else 'false'
        is_flash = 'true' if item.get('isFlash', False) else 'false'
        stock = 15 if idx % 3 != 0 else 4
        
        tags_var = f'Arrays.asList("{cat_name.lower().replace("&", "and")}", "{brand.lower()}", "{sub.lower()}", "luxury")'

        line = f"""        list.add(Product.builder()
                .id("{pid}").sku("{sku}").name("{name}").brand("{brand}")
                .category("{cat_name}").subcategory("{sub}")
                .price({price}).originalPrice({orig}).discountPercentage({disc})
                .rating({rating}).reviewCount({rev})
                .description("{escape_java(desc)}")
                .specifications({specs})
                .sizeAndFit("Standard fit.")
                .shippingInfo("Complimentary shipping over ₹999.")
                .returnPolicy("Easy 7-day doorstep pickup returns.")
                .colorOptions({colors_java})
                .sizes({sizes_java})
                .images(Arrays.asList("{img1}", "{img2}"))
                .hoverImage("{img2}")
                .isNew({is_new}).isTrending({is_trend}).isBestSeller({is_best}).isFlashSale({is_flash})
                .flashSaleEndTime("2026-12-31T23:59:59Z")
                .inStock(true).stockQuantity({stock})
                .tags({tags_var})
                .build());"""
        code_lines.append(line)
    return "\n".join(code_lines)

women_code = generate_product_code(
    parse_items('womenItems'), 'Women', 'wom',
    'Crafted with immaculate precision and editorial artistry. Designed for contemporary luxury, breathability, and lasting elegance.',
    'Map.of("Material", "Italian Mulberry Silk & Fine Wool", "Fit", "Tailored European", "Care", "Dry Clean Only")',
    'Arrays.asList(new ColorOption("Onyx Black", "#121212", null), new ColorOption("Ivory Cream", "#f5f4ef", null), new ColorOption("Emerald Green", "#046307", null))',
    'Arrays.asList("XS", "S", "M", "L", "XL")'
)

men_code = generate_product_code(
    parse_items('menItems'), 'Men', 'men',
    'Handcrafted tailored menswear combining Italian fabrics, ergonomics, and timeless luxury silhouettes.',
    'Map.of("Material", "100% Cotton & Cashmere", "Fit", "Modern Tailored", "Care", "Machine Wash Cold")',
    'Arrays.asList(new ColorOption("Charcoal Grey", "#333333", null), new ColorOption("Oatmeal Beige", "#d4c5b9", null), new ColorOption("Navy Blue", "#1b2a4a", null))',
    'Arrays.asList("S", "M", "L", "XL", "XXL")'
)

kids_code = generate_product_code(
    parse_items('kidsItems'), 'Kids', 'kid',
    'Hypoallergenic organic materials crafted for high active comfort and adorable luxury aesthetic.',
    'Map.of("Material", "100% Organic GOTS Certified Cotton", "Age", "2-10 Years")',
    'Arrays.asList(new ColorOption("Sky Blue", "#87ceeb", null), new ColorOption("Blush Pink", "#ffb6c1", null))',
    'Arrays.asList("2-3Y", "4-5Y", "6-7Y", "8-9Y")'
)

beauty_code = generate_product_code(
    parse_items('beautyItems'), 'Beauty & Self Care', 'bt-400',
    'Dermatologically tested clean formulation enriched with botanical actives and peptides for visible radiance.',
    'Map.of("Volume", "50ml - 500ml", "Formulation", "100% Vegan & Cruelty-Free")',
    'Collections.emptyList()',
    'Arrays.asList("Standard", "Travel Size")'
)

elec_code = generate_product_code(
    parse_items('electronicsItems'), 'Electronics', 'el-500',
    'Engineered for audiophiles and digital creators featuring aircraft-grade metals, lossless codecs, and long battery autonomy.',
    'Map.of("Connectivity", "Bluetooth 5.3 & USB-C Fast Charge", "Warranty", "2 Year International Warranty")',
    'Arrays.asList(new ColorOption("Matte Gunmetal", "#4a4e51", null), new ColorOption("Silver Frost", "#d8dbdd", null), new ColorOption("Midnight Black", "#0e0e0e", null))',
    'Arrays.asList("Standard")'
)

home_code = generate_product_code(
    parse_items('homeItems'), 'Home & Lifestyle', 'hm-600',
    'Hand-crafted artisanal home statement pieces engineered with organic stoneware, solid woods, and brushed brass.',
    'Map.of("Origin", "Hand-crafted Artisanal", "Care", "Wipe Clean / Hand Wash")',
    'Arrays.asList(new ColorOption("Sandstone Ivory", "#e5dec9", null), new ColorOption("Terracotta", "#b35d46", null))',
    'Arrays.asList("Standard", "Large")'
)

java_file_content = f"""package com.logancee.globalcart.service;

import com.logancee.globalcart.model.*;
import java.util.*;

public class ProductSeedData {{

{categories_code}

{brands_code}

    public static List<Product> getProducts() {{
        List<Product> list = new ArrayList<>();
{women_code}
{men_code}
{kids_code}
{beauty_code}
{elec_code}
{home_code}
        return list;
    }}
}}
"""

with open(r'C:\Users\karth\.gemini\antigravity\scratch\logancee-backend\src\main\java\com\logancee\globalcart\service\ProductSeedData.java', 'w', encoding='utf-8') as f:
    f.write(java_file_content)

print("ProductSeedData.java generated successfully!")
