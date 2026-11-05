#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script d'automatisation pour la création des tickets (issues) GitHub
sur le dépôt blingard/sodepa_backend pour le Module de Gestion Budgétaire de l'ERP SODEPA.

Usage:
  python create_github_issues.py --dry-run
  python create_github_issues.py --token <VOTRE_GITHUB_TOKEN>
  (ou définir la variable d'environnement GITHUB_TOKEN)
"""

import os
import sys
import json
import re
import argparse
import urllib.request
import urllib.error
import subprocess
import time

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

REPO = "blingard/sodepa_backend"
API_BASE = f"https://api.github.com/repos/{REPO}"

MILESTONES = [
    {"title": "Phase 1 - Socle & Contrôle (P0/P1)", "description": "Socle, axes, enveloppes et moteur de contrôle des dépenses atomique"},
    {"title": "Phase 2 - Rapprochement & Pilotage (P1)", "description": "Rapprochement comptable, clôtures, comparatifs temps réel et piste d'audit"},
    {"title": "Phase 3 - Budgets Métiers & Prévisions (P2)", "description": "Masse salariale, CAPEX, trésorerie, reforecast, scénarios et administration no-code"},
    {"title": "Module Organigramme & Structure Organisationnelle", "description": "Modélisation hiérarchique de la SODEPA (Siège, Ranches, Abattoirs, Magasins), postes, affectations et résolution N+1"}
]

LABELS = [
    {"name": "epic:socle", "color": "4f46e5", "description": "Socle budgétaire, référentiels et axes"},
    {"name": "epic:organigramme", "color": "06b6d4", "description": "Organigramme, unités organisationnelles, postes et hiérarchie"},
    {"name": "epic:controle-depenses", "color": "dc2626", "description": "Réservations atomiques et prévention des dépassements"},
    {"name": "epic:rapprochement", "color": "2563eb", "description": "Intégration comptable, événements et clôtures"},
    {"name": "epic:pilotage-audit", "color": "0d9488", "description": "Comparatifs, analyse d'écarts et piste d'audit"},
    {"name": "epic:budgets-metiers", "color": "ea580c", "description": "Budgets RH, investissements et trésorerie"},
    {"name": "epic:previsions-consolidation", "color": "7c3aed", "description": "Reforecast, rolling forecast et consolidation"},
    {"name": "epic:administration", "color": "475569", "description": "Administration no-code et supervision"},
    {"name": "priority:P0", "color": "b91c1c", "description": "Priorité P0 : Bloquant / Socle indispensable"},
    {"name": "priority:P1", "color": "e11d48", "description": "Priorité P1 : Obligatoire pour le contrôle et l'audit"},
    {"name": "priority:P2", "color": "d97706", "description": "Priorité P2 : Avancé / Métiers complémentaires"},
    {"name": "priority:P3", "color": "64748b", "description": "Priorité P3 : Évolution"},
    {"name": "type:feature", "color": "0284c7", "description": "Nouvelle fonctionnalité"},
    {"name": "type:refactor", "color": "ca8a04", "description": "Refactoring / Évolution de code existant"},
    {"name": "type:security", "color": "7c3aed", "description": "Sécurité, droits d'accès ou Maker-Checker"},
    {"name": "type:integration", "color": "059669", "description": "Intégration externe ou intergiciel"},
    {"name": "type:technical", "color": "475569", "description": "Tâche d'infrastructure technique / résilience"}
]

def get_git_credential_token():
    try:
        proc = subprocess.Popen(
            ["git", "credential", "fill"],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        out, _ = proc.communicate(input="protocol=https\nhost=github.com\n\n", timeout=5)
        for line in out.splitlines():
            if line.startswith("password="):
                return line.split("=", 1)[1].strip()
    except Exception:
        pass
    return None

def make_request(url, method="GET", token=None, payload=None):
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "Antigravity-Sodepa-IssueCreator/1.0",
        "X-GitHub-Api-Version": "2022-11-28"
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
        
    data = json.dumps(payload).encode("utf-8") if payload else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            content = resp.read().decode("utf-8")
            return json.loads(content) if content else {}
    except urllib.error.HTTPError as e:
        err_msg = e.read().decode("utf-8")
        print(f"❌ Erreur HTTP {e.code} sur {url} : {err_msg}")
        return None
    except Exception as e:
        print(f"❌ Erreur de connexion : {e}")
        return None

def parse_backlog(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    issues = []
    # Pattern to match: ### Issue #X : [TITLE]
    sections = re.split(r"\n### Issue #\d+ : ", content)
    header = sections[0]
    
    for sec in sections[1:]:
        lines = sec.split("\n")
        title = lines[0].strip()
        body_text = "\n".join(lines[1:]).strip()
        
        # Extract milestone
        milestone = None
        m_match = re.search(r"\*\*Milestone\s*:\*\*\s*`([^`]+)`", body_text)
        if m_match:
            milestone = m_match.group(1).strip()
            
        # Extract labels
        labels = []
        l_match = re.search(r"\*\*Labels\s*:\*\*\s*(.+)", body_text)
        if l_match:
            raw_labels = l_match.group(1)
            labels = [lbl.replace("`", "").strip() for lbl in raw_labels.split(",") if lbl.strip()]
            
        issues.append({
            "title": title,
            "body": body_text,
            "milestone": milestone,
            "labels": labels
        })
    return issues

def main():
    parser = argparse.ArgumentParser(description="Création automatisée des tickets GitHub ERP SODEPA")
    parser.add_argument("--token", help="GitHub Personal Access Token (PAT) avec droits repo")
    parser.add_argument("--dry-run", action="store_true", help="Afficher les tickets sans appeler l'API GitHub")
    parser.add_argument("--milestones-only", action="store_true", help="Créer uniquement les jalons (milestones)")
    parser.add_argument("--backlog", default=r"c:\Users\GLC\Documents\sodepa\erp\docs\architecture\BACKLOG_MODULE_BUDGETAIRE_GITHUB_ISSUES.md")
    args = parser.parse_args()

    token = args.token or os.environ.get("GITHUB_TOKEN") or get_git_credential_token()
    
    if not os.path.exists(args.backlog):
        print(f"❌ Fichier backlog introuvable : {args.backlog}")
        sys.exit(1)
        
    issues = parse_backlog(args.backlog)
    print(f"📋 {len(issues)} tickets détectés dans le backlog.\n")

    if args.dry_run or not token:
        print("🔍 MODE SIMULATION (DRY-RUN)")
        if not token and not args.dry_run:
            print("💡 Aucun jeton GitHub trouvé. Passage automatique en mode --dry-run.\n")
            
        print("================ MILESTONES À CRÉER ================")
        for m in MILESTONES:
            print(f"• [Milestone] {m['title']}")
            
        print("\n================ LABELS À CRÉER ================")
        for l in LABELS:
            print(f"• [Label] {l['name']} (#{l['color']}) - {l['description']}")
            
        print("\n================ EXEMPLE DE TICKETS ================")
        for idx, issue in enumerate(issues, start=1):
            print(f"#{idx:02d} : {issue['title']}")
            print(f"     Milestone : {issue['milestone']}")
            print(f"     Labels    : {', '.join(issue['labels'])}")
            first_lines = "\n     ".join(issue['body'].split("\n")[:4])
            print(f"     Aperçu    : {first_lines}...\n")
            
        print("✅ Simulation terminée avec succès.")
        return

    print(f"🚀 Connexion au dépôt GitHub : {REPO}")
    
    # 1. Gestion des Milestones
    print("\n📌 Synchronisation des Milestones...")
    existing_milestones = make_request(f"{API_BASE}/milestones?state=all", token=token) or []
    milestone_map = {m["title"]: m["number"] for m in existing_milestones}
    
    for m in MILESTONES:
        if m["title"] in milestone_map:
            print(f"  ✓ Milestone existante : {m['title']} (#{milestone_map[m['title']]})")
        else:
            created = make_request(f"{API_BASE}/milestones", method="POST", token=token, payload=m)
            if created:
                milestone_map[m["title"]] = created["number"]
                print(f"  ➕ Milestone créée : {m['title']} (#{created['number']})")
            time.sleep(0.5)

    if args.milestones_only:
        print("\n🎉 Jalons (Milestones) synchronisés avec succès sur GitHub !")
        return

    # 2. Gestion des Labels
    print("\n🏷️ Synchronisation des Labels...")
    existing_labels = make_request(f"{API_BASE}/labels?per_page=100", token=token) or []
    existing_label_names = {l["name"].lower() for l in existing_labels}
    
    for l in LABELS:
        if l["name"].lower() in existing_label_names:
            print(f"  ✓ Label existant : {l['name']}")
        else:
            make_request(f"{API_BASE}/labels", method="POST", token=token, payload=l)
            print(f"  ➕ Label créé : {l['name']}")
            time.sleep(0.3)

    # 3. Création des Issues
    print(f"\n📝 Création des {len(issues)} Issues GitHub...")
    for idx, issue in enumerate(issues, start=1):
        payload = {
            "title": issue["title"],
            "body": issue["body"],
            "labels": issue["labels"]
        }
        if issue["milestone"] and issue["milestone"] in milestone_map:
            payload["milestone"] = milestone_map[issue["milestone"]]
            
        created_issue = make_request(f"{API_BASE}/issues", method="POST", token=token, payload=payload)
        if created_issue:
            print(f"  ✅ #{idx:02d} Créé : {created_issue.get('html_url')} — {issue['title']}")
        else:
            print(f"  ❌ #{idx:02d} Échec de création : {issue['title']}")
            
        time.sleep(1) # Éviter le rate limit secondaire de GitHub

    print("\n🎉 Toutes les issues ont été traitées avec succès sur GitHub !")

if __name__ == "__main__":
    main()
