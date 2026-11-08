import os
import re

controllers = []
for root, dirs, files in os.walk('src/main/java/com/sodepa/erp'):
    for f in files:
        if f.endswith('Controller.java'):
            path = os.path.join(root, f)
            with open(path, 'r', encoding='utf-8', errors='ignore') as jf:
                content = jf.read()
            m = re.findall(r'@RequestMapping\(.*?\"([^\"]+)\"', content)
            controllers.append((f, m[0] if m else 'NONE', path))

controllers.sort(key=lambda x: x[1])
for c, m, p in controllers:
    print(f'{c:<45} {m}')
print(f'Total controllers: {len(controllers)}')
