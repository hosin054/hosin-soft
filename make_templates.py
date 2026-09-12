# -*- coding: utf-8 -*-
import os
import shutil
import generate_excel

# 1. Products Template
product_headers = [
    "اسم المنتج",
    "الباركود",
    "التصنيف",
    "الوحدة الكبرى",
    "الوحدة الصغرى",
    "معامل التحويل",
    "سعر الشراء",
    "سعر البيع النقدي",
    "سعر البيع الآجل",
    "الكمية الحالية",
    "حد التنبيه"
]

product_rows = [
    ["عصير تفاح طبيعي 1 لتر", "6281009901", "عصائر ومشروبات", "كرتون", "علبة", "12", "45.0", "5.0", "5.5", "120", "20"],
    ["بسكويت شاي مالح 100جم", "6281009902", "حلويات وبسكويت", "كرتون", "باكيت", "24", "36.0", "2.0", "2.25", "240", "48"],
    ["أرز بسمتي فاخر 5 كجم", "6281009903", "مواد غذائية", "كيس", "كيلو", "5", "35.0", "42.0", "45.0", "50", "10"],
    ["زيت زيتون بكر ممتاز 500مل", "6281009904", "زيوت ودهون", "كرتون", "زجاجة", "12", "120.0", "14.0", "15.0", "36", "6"],
    ["حليب مجفف سريع الذوبان 900جم", "6281009905", "ألبان وأجبان", "كرتون", "علبة", "6", "150.0", "32.0", "34.0", "30", "5"],
    ["شاي أحمر خشن 200جم", "6281009906", "مشروبات ساخنة", "كرتون", "علبة", "20", "80.0", "5.5", "6.0", "60", "12"],
    ["معجون طماطم مركز 135جم", "6281009907", "معلبات", "شدّة", "علبة", "8", "12.0", "2.0", "2.25", "80", "16"],
    ["مناديل وجه ناعمة 200 منديل", "6281009908", "منظفات وعناية", "كرتون", "علبة", "10", "25.0", "3.5", "4.0", "50", "10"]
]

# 2. Customers Template
customer_headers = [
    "اسم العميل",
    "رقم الهاتف",
    "العنوان / المدينة",
    "سقف الائتمان",
    "الرصيد الافتتاحي"
]

customer_rows = [
    ["شركة الأمل للتجارة والتوزيع", "0551234567", "الرياض - حي الملز", "10000", "0"],
    ["مؤسسة النور للمقاولات", "0509876543", "جدة - طريق الملك", "5000", "500"],
    ["سوبرماركت البركة", "0543216789", "الدمام - السوق المركزي", "15000", "1200"],
    ["مطعم ومطبخ الضيافة", "0567891234", "المدينة المنورة", "8000", "0"],
    ["أحمد عبد الله القحطاني", "0531122334", "الرياض - حي النسيم", "2000", "0"]
]

def write_csv_with_bom(filepath, headers, rows):
    with open(filepath, 'w', encoding='utf-8-sig') as f:
        f.write(",".join(headers) + "\n")
        for r in rows:
            clean_row = []
            for cell in r:
                c = str(cell).replace('"', '""')
                if ',' in c or '\n' in c or '"' in c:
                    clean_row.append(f'"{c}"')
                else:
                    clean_row.append(c)
            f.write(",".join(clean_row) + "\n")

# Ensure directories
os.makedirs("apk", exist_ok=True)

# Generate XLSX files
generate_excel.create_simple_xlsx("قالب_استيراد_المنتجات.xlsx", "المنتجات", product_headers, product_rows)
generate_excel.create_simple_xlsx("قالب_استيراد_العملاء.xlsx", "العملاء", customer_headers, customer_rows)

# Generate CSV files with UTF-8 BOM
write_csv_with_bom("قالب_استيراد_المنتجات.csv", product_headers, product_rows)
write_csv_with_bom("قالب_استيراد_العملاء.csv", customer_headers, customer_rows)

# English aliases
generate_excel.create_simple_xlsx("products_import_template.xlsx", "Products", product_headers, product_rows)
generate_excel.create_simple_xlsx("customers_import_template.xlsx", "Customers", customer_headers, customer_rows)
write_csv_with_bom("products_import_template.csv", product_headers, product_rows)
write_csv_with_bom("customers_import_template.csv", customer_headers, customer_rows)

# Copy to apk/ folder
for fname in [
    "قالب_استيراد_المنتجات.xlsx",
    "قالب_استيراد_المنتجات.csv",
    "قالب_استيراد_العملاء.xlsx",
    "قالب_استيراد_العملاء.csv",
    "products_import_template.xlsx",
    "products_import_template.csv",
    "customers_import_template.xlsx",
    "customers_import_template.csv"
]:
    shutil.copy2(fname, os.path.join("apk", fname))

print("Templates created successfully in both . and ./apk")
