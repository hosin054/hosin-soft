import zipfile
import io
import os

def create_simple_xlsx(filename, sheet_name, headers, rows):
    # XML templates
    content_types = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStringTable+xml"/>
</Types>"""

    root_rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    workbook_xml = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="{sheet_name}" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    workbook_rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""

    styles_xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="12"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1E3A8A"/></patternFill></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
  </cellXfs>
</styleSheet>"""

    # Collect shared strings
    shared_strings = []
    string_map = {}
    
    def get_string_id(text):
        text = str(text)
        if text not in string_map:
            string_map[text] = len(shared_strings)
            shared_strings.append(text)
        return string_map[text]

    def col_letter(col_idx):
        result = ""
        while col_idx >= 0:
            result = chr(col_idx % 26 + ord('A')) + result
            col_idx = col_idx // 26 - 1
        return result

    # Build sheet XML
    sheet_rows = []
    
    # Header row
    h_cells = []
    for col_idx, h in enumerate(headers):
        ref = f"{col_letter(col_idx)}1"
        s_id = get_string_id(h)
        h_cells.append(f'<c r="{ref}" t="s" s="1"><v>{s_id}</v></c>')
    sheet_rows.append(f'<row r="1">{"".join(h_cells)}</row>')

    # Data rows
    for r_idx, row in enumerate(rows, start=2):
        r_cells = []
        for col_idx, val in enumerate(row):
            ref = f"{col_letter(col_idx)}{r_idx}"
            # Check if numeric
            try:
                float_val = float(val)
                # It's numeric if not empty and doesn't look like barcode with leading zero
                val_str = str(val).strip()
                if val_str.startswith("0") and len(val_str) > 1 and not val_str.startswith("0."):
                    # treat as string (like phone or barcode)
                    s_id = get_string_id(val_str)
                    r_cells.append(f'<c r="{ref}" t="s"><v>{s_id}</v></c>')
                elif val_str.isdigit() or (val_str.replace('.', '', 1).isdigit() and val_str.count('.') == 1):
                    r_cells.append(f'<c r="{ref}"><v>{val_str}</v></c>')
                else:
                    s_id = get_string_id(val_str)
                    r_cells.append(f'<c r="{ref}" t="s"><v>{s_id}</v></c>')
            except ValueError:
                s_id = get_string_id(str(val))
                r_cells.append(f'<c r="{ref}" t="s"><v>{s_id}</v></c>')
        sheet_rows.append(f'<row r="{r_idx}">{"".join(r_cells)}</row>')

    sheet_xml = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView tabSelected="1" workbookViewId="0" rightToLeft="1"/>
  </sheetViews>
  <sheetData>
    {"".join(sheet_rows)}
  </sheetData>
</worksheet>"""

    sst_entries = "".join([f"<si><t>{s}</t></si>" for s in shared_strings])
    sst_xml = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="{len(shared_strings)}" uniqueCount="{len(shared_strings)}">
  {sst_entries}
</sst>"""

    with zipfile.ZipFile(filename, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", content_types)
        z.writestr("_rels/.rels", root_rels)
        z.writestr("xl/workbook.xml", workbook_xml)
        z.writestr("xl/_rels/workbook.xml.rels", workbook_rels)
        z.writestr("xl/styles.xml", styles_xml)
        z.writestr("xl/sharedStrings.xml", sst_xml)
        z.writestr("xl/worksheets/sheet1.xml", sheet_xml)

print("Script template ready")
