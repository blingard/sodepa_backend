import os
import re
import sys

# Dictionnaire de traduction sémantique métier SODEPA pour les champs
TERM_MAP = {
    'id': 'Identifiant unique technique',
    'code': 'Code de référence unique normalisé',
    'libelle': 'Libellé descriptif officiel',
    'intitule': 'Intitulé officiel ou désignation',
    'designation': 'Désignation complète du produit, service ou élément',
    'nom': 'Nom officiel',
    'prenom': 'Prénom de la personne',
    'description': 'Description détaillée et informations contextuelles',
    'commentaire': 'Commentaires et observations opérationnelles',
    'remarque': 'Remarques éventuelles de gestion',
    'motif': 'Motif justificatif de l\'opération ou de la décision',
    'notes': 'Notes d\'instruction ou justifications administratives',
    'statut': 'Statut dans le cycle de vie de gestion au sein de la SODEPA',
    'etat': 'État opérationnel ou statut courant',
    'actif': 'Indicateur d\'activation opérationnelle (true si actif, false si archivé/désactivé)',
    'bloque': 'Indicateur de blocage administratif ou financier',
    'supprime': 'Indicateur de suppression logique (soft delete)',
    'date': 'Date calendaire de l\'opération',
    'datedebut': 'Date de début d\'effet ou de validité',
    'datefin': 'Date de fin d\'effet ou échéance',
    'datecreation': 'Date et heure de création de l\'enregistrement',
    'datemodification': 'Date et heure de dernière mise à jour',
    'datevalidation': 'Date et heure de validation hiérarchique',
    'dateecheance': 'Date d\'échéance contractuelle ou réglementaire',
    'montant': 'Montant financier exprimé en Francs CFA (XAF)',
    'montanttotal': 'Montant total global exprimé en Francs CFA (XAF)',
    'montantht': 'Montant hors taxes en Francs CFA (XAF)',
    'montantttc': 'Montant toutes taxes comprises en Francs CFA (XAF)',
    'montanttva': 'Montant de la TVA appliquée en Francs CFA (XAF)',
    'montantir': 'Montant de la retenue à la source (Acompte IR / TSR) en Francs CFA',
    'solde': 'Solde financier disponible ou restant dû en Francs CFA',
    'prix': 'Prix unitaire appliqué en Francs CFA',
    'prixunitaire': 'Prix unitaire en Francs CFA',
    'cout': 'Coût d\'acquisition ou de revient en Francs CFA',
    'plafond': 'Plafond financier maximal autorisé en Francs CFA',
    'taux': 'Taux ou pourcentage appliqué',
    'tauxtva': 'Taux de taxe sur la valeur ajoutée (TVA)',
    'pourcentage': 'Pourcentage appliqué à l\'opération',
    'quantite': 'Quantité ou volume exprimé dans l\'unité de mesure de référence',
    'qte': 'Quantité unitaire ou nombre d\'unités',
    'poids': 'Poids mesuré en kilogrammes (kg) pour le bétail ou la carcasse',
    'poidsvif': 'Poids vif de l\'animal mesuré sur pied au ranch ou à l\'entrée de l\'abattoir',
    'poidscarcasse': 'Poids net de la carcasse après abattage et ressuage',
    'rendement': 'Taux de rendement à l\'abattage ou coefficient de carcasse',
    'unite': 'Unité de mesure de référence (kg, tête, litre, sac, unité)',
    'unitemesure': 'Unité de mesure réglementaire',
    'type': 'Type de classification ou catégorie selon la nomenclature SODEPA',
    'categorie': 'Catégorie de rattachement administratif ou technique',
    'classe': 'Classe de classification ou grade hiérarchique',
    'matricule': 'Matricule officiel de l\'employé SODEPA',
    'email': 'Adresse électronique professionnelle de contact',
    'telephone': 'Numéro de téléphone de contact',
    'adresse': 'Adresse géographique ou postale',
    'ville': 'Ville ou agglomération de rattachement',
    'region': 'Région administrative du Cameroun (ex: Adamaoua, Nord, Extrême-Nord, Centre)',
    'site': 'Site pastoral ou unité d\'exploitation SODEPA (ex: Ranch de Faro, Dumbo, Jakiri)',
    'siteid': 'Identifiant du site d\'exploitation pastoral ou abattoir SODEPA',
    'ranch': 'Ranch d\'élevage ou station pastorale de la SODEPA',
    'abattoir': 'Abattoir frigorifique ou unité d\'abattage industrielle (Yaoundé, Douala, Ngaoundéré)',
    'lot': 'Numéro de lot pour la traçabilité de production ou sanitaire',
    'espece': 'Espèce animale concernée (Bovine, Ovine, Caprine, Porcine)',
    'race': 'Race animale élevée ou abattue (Goudali, Mbororo, Charolais, etc.)',
    'sexe': 'Sexe de l\'animal (Mâle, Femelle)',
    'age': 'Âge mesuré en mois ou années',
    'cheptel': 'Identifiant ou référence du cheptel pastoral suivi',
    'fournisseur': 'Fournisseur partenaire ou adjudicataire de la commande',
    'fournisseurid': 'Identifiant unique du fournisseur sélectionné',
    'client': 'Client acheteur ou partenaire commercial sous contrat',
    'clientid': 'Identifiant unique du client acheteur',
    'employe': 'Employé ou collaborateur SODEPA concerné',
    'employeid': 'Identifiant unique de l\'employé concerné',
    'demandeur': 'Agent ou responsable ayant initié la demande',
    'demandeurid': 'Identifiant de l\'employé initiateur de la demande',
    'validateur': 'Autorité ou supérieur hiérarchique ayant validé l\'étape',
    'compte': 'Compte général du plan comptable SYSCOHADA de la SODEPA',
    'journal': 'Code du journal comptable d\'imputation (ex: ACH, VTE, BQ, CA, OD)',
    'exercice': 'Exercice comptable ou budgétaire annuel de rattachement',
    'budget': 'Enveloppe budgétaire ou ligne de crédit allouée',
    'decision': 'Décision prise (APPROUVE, REJETE, RETOUR_MODIFICATION)',
    'action': 'Action métier ou technique déclenchée',
    'workflow': 'Instance ou définition de circuit de validation workflow',
    'fichier': 'Fichier joint ou pièce justificative archivée dans la GED',
    'hash': 'Empreinte cryptographique SHA-256 scellant l\'intégrité',
    'token': 'Jeton de sécurité ou d\'authentification chiffré',
    'permissions': 'Ensemble des permissions d\'accès et droits attribués',
    'roles': 'Rôles ou profils fonctionnels de sécurité IAM',
}

def clean_camel_case(s):
    words = re.findall(r'[A-Z]?[a-z]+|[A-Z]+(?=[A-Z][a-z]|\d|\b)|[0-9]+', s)
    return [w.lower() for w in words]

def generate_field_description(field_name, field_type, class_name):
    lower_name = field_name.lower()
    if lower_name in TERM_MAP:
        return f"{TERM_MAP[lower_name]} dans le cadre des opérations SODEPA."

    words = clean_camel_case(field_name)
    combined = "".join(words)
    if combined in TERM_MAP:
        return f"{TERM_MAP[combined]} dans le cadre des opérations SODEPA."

    # Analyser par motifs de fin ou de début
    if lower_name.endswith('id'):
        base = lower_name[:-2]
        base_desc = TERM_MAP.get(base, base)
        return f"Identifiant unique technique (UUID) de l'élément {base_desc} associé."
    elif lower_name.endswith('nom') or lower_name.endswith('name'):
        base = lower_name[:-3]
        return f"Nom ou désignation textuelle de {base} pour l'affichage et la traçabilité."
    elif lower_name.endswith('code'):
        return f"Code normalisé unique identifiant {field_name} dans le référentiel SODEPA."
    elif lower_name.endswith('date'):
        return f"Date d'enregistrement ou d'échéance relative à {field_name}."
    elif lower_name.endswith('statut') or lower_name.endswith('status'):
        return f"Statut opérationnel de {field_name} selon le workflow de gestion SODEPA."
    elif lower_name.startswith('is') or lower_name.startswith('has'):
        return f"Indicateur booléen spécifiant si {field_name} est effectif ou applicable."
    elif 'montant' in words or 'prix' in words or 'cout' in words:
        return f"Valeur financière ({' '.join(words)}) exprimée en Francs CFA (XAF)."
    elif 'taux' in words or 'pourcentage' in words:
        return f"Taux ou pourcentage ({' '.join(words)}) applicable au calcul."

    phrase = " ".join(words)
    return f"Champ {field_name} : {phrase} utilisé pour le pilotage et le suivi dans {class_name}."

def process_java_file(filepath):
    with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()

    original_content = content
    rel_path = os.path.relpath(filepath).replace('\\', '/')

    # Déterminer si c'est une entité, request, input, etc.
    is_entity = 'entities' in filepath
    is_request = 'requests' in filepath or 'inputs' in filepath or 'payload' in filepath

    # 1. Vérifier si la classe a une Javadoc de niveau classe
    class_match = re.search(r'(?m)^(?:@.*\n)*public\s+(?:class|interface|enum|record)\s+(\w+)', content)
    if class_match:
        class_name = class_match.group(1)
        start_idx = class_match.start()
        # Regarder si avant start_idx il y a /** ... */
        prefix = content[:start_idx].rstrip()
        if not prefix.endswith('*/'):
            # Générer une Javadoc pour la classe
            if is_entity:
                desc = f"Entité de persistance représentant `{class_name}` au sein du système ERP SODEPA.\n * Assure le stockage relationnel et la traçabilité des données du domaine."
            elif is_request:
                desc = f"Objet de transfert de données (DTO / Requête) pour l'opération `{class_name}` dans l'ERP SODEPA.\n * Encapsule les paramètres requis avec leurs règles de validation."
            elif 'Controller' in class_name:
                desc = f"Contrôleur REST exposant les points de terminaison pour `{class_name}`.\n * Permet la gestion sécurisée des opérations de l'ERP SODEPA."
            elif 'Service' in class_name or 'UseCase' in class_name:
                desc = f"Service métier d'application implémentant la logique et les règles de gestion pour `{class_name}` au sein de la SODEPA."
            elif 'Repository' in class_name or 'Repo' in class_name:
                desc = f"Dépôt Spring Data JPA pour l'accès aux données et la persistance de `{class_name}`."
            else:
                desc = f"Composant applicatif `{class_name}` du système ERP SODEPA."

            class_javadoc = f"/**\n * {desc}\n */\n"
            content = content[:start_idx] + class_javadoc + content[start_idx:]

    # 2. Si c'est une entité ou request/input, documenter chaque champ s'il ne l'est pas déjà
    if is_entity or is_request:
        lines = content.splitlines(keepends=True)
        new_lines = []
        i = 0
        in_multiline_comment = False
        current_class = class_match.group(1) if class_match else "SODEPA"

        # Regex pour champ: ex: private String nom; private Long id; private BigDecimal montant;
        field_pattern = re.compile(r'^\s+(?:private|protected|public)\s+([\w<>,\s\?\[\]]+)\s+(\w+)\s*(?:=.*?)?;\s*$')
        record_param_pattern = re.compile(r'^\s*(@[\w\(\)\s",=.]+?\s+)?([\w<>,\s\?\[\]]+)\s+(\w+)\s*(?:,)?\s*$')

        while i < len(lines):
            line = lines[i]
            stripped = line.strip()

            if '/*' in stripped:
                in_multiline_comment = True
            if '*/' in stripped:
                in_multiline_comment = False
                new_lines.append(line)
                i += 1
                continue

            if not in_multiline_comment:
                # Vérifier si c'est une déclaration de champ
                fm = field_pattern.match(line)
                if fm:
                    field_type = fm.group(1).strip()
                    field_name = fm.group(2).strip()

                    # Vérifier si les lignes précédentes contiennent déjà une javadoc
                    # Regarder en arrière à travers les annotations
                    has_javadoc = False
                    back_idx = len(new_lines) - 1
                    while back_idx >= 0:
                        prev = new_lines[back_idx].strip()
                        if prev.startswith('@'):
                            back_idx -= 1
                        elif prev.endswith('*/'):
                            has_javadoc = True
                            break
                        else:
                            break

                    if not has_javadoc:
                        # Déterminer où insérer la javadoc (au-dessus des annotations du champ)
                        insert_pos = len(new_lines)
                        while insert_pos > 0 and new_lines[insert_pos - 1].strip().startswith('@'):
                            insert_pos -= 1

                        indent = "    "
                        field_desc = generate_field_description(field_name, field_type, current_class)
                        doc_block = f"{indent}/**\n{indent} * {field_desc}\n{indent} */\n"
                        new_lines.insert(insert_pos, doc_block)

            new_lines.append(line)
            i += 1

        content = "".join(new_lines)

    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

if __name__ == '__main__':
    modified_count = 0
    for root, dirs, files in os.walk('src/main/java/com/sodepa/erp'):
        for file in files:
            if file.endswith('.java'):
                fp = os.path.join(root, file)
                if process_java_file(fp):
                    modified_count += 1
    print(f'Done. Enriched Javadoc in {modified_count} files.')
