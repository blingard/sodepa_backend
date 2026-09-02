import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { endpoints } from '../../src/api/endpoints';
import { HttpMethod } from '../../src/api/http';

/**
 * Garde-fou de couverture : compare les routes déclarées par les contrôleurs
 * Spring aux routes présentes dans `src/api/endpoints.ts`.
 *
 * Ce test ne sollicite pas le backend : il lit les sources Java du module
 * parent. Toute route ajoutée côté backend fait échouer ce test tant qu'aucun
 * test ne la couvre.
 */
const SOURCES_JAVA = path.resolve(__dirname, '../../../main/java');

interface RouteJava {
  method: HttpMethod;
  template: string;
  fichier: string;
}

/** Liste récursivement les contrôleurs REST du backend. */
function listerControleurs(racine: string): string[] {
  const resultats: string[] = [];
  for (const entree of fs.readdirSync(racine, { withFileTypes: true })) {
    const complet = path.join(racine, entree.name);
    if (entree.isDirectory()) {
      resultats.push(...listerControleurs(complet));
    } else if (entree.name.endsWith('RestController.java')) {
      resultats.push(complet);
    }
  }
  return resultats;
}

/** Extrait les routes (verbe + gabarit d'URL) d'un contrôleur. */
function extraireRoutes(fichier: string): RouteJava[] {
  const source = fs.readFileSync(fichier, 'utf-8');
  const base = /@RequestMapping\(\s*"([^"]*)"\s*\)/.exec(source)?.[1] ?? '';
  const routes: RouteJava[] = [];

  const motif = /@(Get|Post|Put|Patch|Delete)Mapping(?:\(\s*(?:value\s*=\s*)?"([^"]*)"\s*\))?/g;
  for (let match = motif.exec(source); match !== null; match = motif.exec(source)) {
    const method = match[1].toLowerCase() as HttpMethod;
    const suffixe = match[2] ?? '';
    const normalise = suffixe && !suffixe.startsWith('/') ? `/${suffixe}` : suffixe;
    routes.push({ method, template: `${base}${normalise}`, fichier: path.basename(fichier) });
  }
  return routes;
}

/** Transforme un gabarit Spring (`/api/x/{id}`) en expression régulière. */
function versRegex(template: string): RegExp {
  const echappe = template
    .replace(/[.*+?^${}()|[\]\\]/g, (caractere) => (caractere === '{' || caractere === '}' ? caractere : `\\${caractere}`))
    .replace(/\{[^}]*\}/g, '[^/]+');
  return new RegExp(`^${echappe}$`);
}

test.describe('API — Surface exposée', () => {
  test('chaque route des contrôleurs Spring est couverte par le registre de tests', () => {
    test.skip(!fs.existsSync(SOURCES_JAVA), 'sources Java du backend introuvables');

    const routesJava = listerControleurs(SOURCES_JAVA).flatMap(extraireRoutes);
    expect(routesJava.length, 'aucune route détectée : le parseur est à revoir').toBeGreaterThan(0);

    const nonCouvertes = routesJava.filter((route) => {
      const regex = versRegex(route.template);
      return !endpoints.some((e) => e.method === route.method && regex.test(e.path));
    });

    expect(
      nonCouvertes.map((r) => `${r.method.toUpperCase()} ${r.template} (${r.fichier})`),
      'routes backend absentes de src/api/endpoints.ts',
    ).toEqual([]);
  });

  test('le registre ne référence pas de route inconnue du backend', () => {
    test.skip(!fs.existsSync(SOURCES_JAVA), 'sources Java du backend introuvables');

    const routesJava = listerControleurs(SOURCES_JAVA).flatMap(extraireRoutes);

    const orphelines = endpoints.filter(
      (e) =>
        !routesJava.some((route) => route.method === e.method && versRegex(route.template).test(e.path)),
    );

    expect(
      orphelines.map((e) => `${e.method.toUpperCase()} ${e.path}`),
      'routes du registre qui n’existent plus côté backend',
    ).toEqual([]);
  });
});
