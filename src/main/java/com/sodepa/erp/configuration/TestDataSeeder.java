package com.sodepa.erp.configuration;

import com.sodepa.erp.budget.application.inputs.*;
import com.sodepa.erp.budget.application.usecase.*;
import com.sodepa.erp.budget.infrastructure.entities.*;
import com.sodepa.erp.budget.infrastructure.repo.*;
import com.sodepa.erp.comptabilite.analytique.application.usecase.*;
import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.*;
import com.sodepa.erp.comptabilite.analytique.infrastructure.repo.*;
import com.sodepa.erp.comptabilite.generale.application.inputs.*;
import com.sodepa.erp.comptabilite.generale.application.usecase.*;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.*;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.*;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.ReleveBancaireEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.entities.LigneReleveBancaireEntity;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.*;
import com.sodepa.erp.comptabilite.generale.infrastructure.repo.ReleveBancaireRepository;
import com.sodepa.erp.share.CurrentUserAuthenticationToken;
import com.sodepa.erp.share.MakerCheckerRequestEntity;
import com.sodepa.erp.share.MakerCheckerRequestJpaRepo;
import com.sodepa.erp.share.UserData;
import com.sodepa.erp.user.application.inputs.CreateUserInput;
import com.sodepa.erp.user.infrastructure.adapter.UserAdapter;
import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.user.infrastructure.repo.UserRepository;
import com.sodepa.erp.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Script de peuplement de données de test historique sur 10 ans pour SODEPA ERP.
 * <p>
 * Passe par les cas d'utilisation applicatifs officiels (maker-checker, etc.)
 * et propose un repli robuste en cas d'absence de connectivité (Keycloak, MinIO).
 */
@Component
@Profile("test")
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class TestDataSeeder implements CommandLineRunner {

    // ── Adapteurs (Maker-Checker & Logique métier) ──
    private final JournalAdapter journalAdapter;
    private final BankAdapter bankAdapter;
    private final TiersAdapter tiersAdapter;
    private final UserAdapter userAdapter;
    private final ImmobilisationAdapter immobilisationAdapter;
    private final CompteAdapter compteAdapter;

    // ── Usecases ──
    private final SaisirEcritureUseCase saisirEcritureUseCase;
    private final ValiderEcritureUseCase validerEcritureUseCase;
    private final SoumettreEcritureUseCase soumettreEcritureUseCase;
    private final CreerBudgetPlanUseCase creerBudgetPlanUseCase;
    private final AjouterItemPlanUseCase ajouterItemPlanUseCase;
    private final SoumettrePlanUseCase soumettrePlanUseCase;
    private final ApprouverPlanUseCase approuverPlanUseCase;
    private final EnregistrerFinancementUseCase enregistrerFinancementUseCase;
    private final AjouterPrevisionUseCase ajouterPrevisionUseCase;
    private final EnregistrerCouvertureUseCase enregistrerCouvertureUseCase;
    private final EnregistrerEngagementHorsBilanUseCase enregistrerEngagementHorsBilanUseCase;
    private final EnregistrerEngagementUseCase enregistrerEngagementUseCase;
    private final CleRepartitionUseCase cleRepartitionUseCase;
    private final BudgetUseCase budgetUseCase;
    private final FiscalYearClosingAdapter fiscalYearClosingAdapter;
    private final PlatformTransactionManager transactionManager;

    // ── Repositories (Accès direct pour vérifications et replis) ──
    private final UserRepository userRepository;
    private final MakerCheckerRequestJpaRepo requestRepo;
    private final JournalRepository journalRepository;
    private final BanqueRepository banqueRepository;
    private final TiersRepository tiersRepository;
    private final ImmobilisationRepository immobilisationRepository;
    private final EcritureRepository ecritureRepository;
    private final ReleveBancaireRepository releveBancaireRepository;
    private final AxeAnalytiqueRepository axeRepository;
    private final SectionAnalytiqueRepository sectionRepository;
    private final CleRepartitionRepository cleRepartitionRepository;
    private final BudgetPlanRepository budgetPlanRepository;
    private final LigneDecouvertRepository ligneDecouvertRepository;

    private static final UUID MAKER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CHECKER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static final String[] CAMEROON_LASTNAMES = {
        // 1. Adamaoua
        "Nyako", "Hamadjoda", "Bobbo", "Garga", "Yaya", "Djoulde", "Bello", "Dewa", "Bakari", "Bouba",
        // 2. Centre
        "Atangana", "Owona", "Essomba", "Mbida", "Ondoua", "Mballa", "Bidzogo", "Biloa", "Ngono", "Tsanga",
        // 3. Est
        "Mpouel", "Ndanga", "Mbazo'o", "Ze", "Bilo'o", "Meyanga", "Belinga", "Ndongo", "Mendomo", "Mvondo",
        // 4. Extrême-Nord
        "Boukar", "Adoum", "Gonji", "Vondou", "Tchinda", "Zra", "Wandji", "Tikela", "Djao", "Wassouni",
        // 5. Littoral
        "Epee", "Ndoumbe", "Lobe", "Eteki", "Mpondo", "Moukouri", "Nsame", "Dikongue", "Titi", "Mouelle",
        // 6. Nord
        "Mohamadou", "Sadou", "Iya", "Dairou", "Nana", "Alhadji", "Garga", "Bello", "Ousmane", "Sali",
        // 7. Nord-Ouest
        "Foncha", "Ndi", "Fobi", "Jua", "Mbah", "Ngwa", "Suh", "Bih", "Fru", "Che",
        // 8. Ouest
        "Kamga", "Fotso", "Tagne", "Foko", "Wambo", "Kengne", "Sop", "Defo", "Simo", "Djoko",
        // 9. Sud
        "Obama", "Ndong", "Mengue", "Evina", "Bekolo", "Nnanga", "Ela", "Assoumou", "Medjo", "Angoula",
        // 10. Sud-Ouest
        "Tabot", "Ebai", "Ashu", "Enow", "Manyi", "Taku", "Egbe", "Tambe", "Bessem", "Ayuk"
    };

    private static final String[] CAMEROON_FIRSTNAMES = {
        // 1. Adamaoua
        "Haman", "Balkissou", "Ousmanou", "Fadimatou", "Alim", "Aissatou", "Mohamadou", "Djamilatou", "Halilou", "Nana",
        // 2. Centre
        "Jean-Pierre", "Chantal", "Dieudonné", "Thérèse", "Marc-Aurèle", "Marie-Claire", "Guy-Roger", "Solange", "Christian", "Jacqueline",
        // 3. Est
        "Jean-Marie", "Philomène", "Justin", "Sidonie", "Boniface", "Clarisse", "Apollinaire", "Gertrude", "Blaise", "Emilienne",
        // 4. Extrême-Nord
        "Hamadou", "Fadimatou", "Ousmane", "Hinna", "Bladi", "Aminatou", "Bouba", "Gounoko", "Asta", "Halima",
        // 5. Littoral
        "Samuel", "Charlotte", "Joseph", "Suzanne", "David", "Henriette", "Pierre", "Georgette", "Emmanuel", "Alice",
        // 6. Nord
        "Bello", "Asta", "Aminatou", "Iya", "Dairou", "Nana", "Mohamadou", "Sadou", "Garga", "Sali",
        // 7. Nord-Ouest
        "John", "Comfort", "Augustine", "Beatrice", "Emmanuel", "Evelyn", "Peter", "Grace", "Richard", "Mercy",
        // 8. Ouest
        "Jean-Claude", "Monique", "Pascal", "Florence", "Rodrigue", "Sandrine", "Ghislain", "Colette", "Michel", "Veronique",
        // 9. Sud
        "Paul", "Jeanne", "François", "Chantal", "Marc", "Marie", "Charles", "Solange", "Pierre", "Therese",
        // 10. Sud-Ouest
        "Daniel", "Elizabeth", "Christopher", "Patricia", "George", "Sarah", "William", "Catherine", "Thomas", "Martha"
    };

    private final Map<String, UUID> bankIds = new HashMap<>();
    private final Map<String, UUID> tiersIds = new HashMap<>();
    private final Map<String, UUID> journalIds = new HashMap<>();
    private SectionAnalytiqueEntity secNgd, secDla, secYde, secBov, secOvi, secAvi;

    @Override
    public void run(String... args) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("  Démarrage du peuplement complet SODEPA (Historique 10 ans)");
        log.info("═══════════════════════════════════════════════════════════════");

        try {
            seedRefData();
            seedHistoricalData();
            seedTreasuryData();
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("  Peuplement des données de test terminé avec succès ✓");
            log.info("═══════════════════════════════════════════════════════════════");
        } catch (Exception e) {
            log.error("Erreur lors de l'exécution du seeder de test : ", e);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(String username, UUID userId) {
        UserData userData = UserData.builder()
                .username(username)
                .userId(userId.toString())
                .permissions(new HashSet<>(Arrays.asList(Permissions.values())))
                .sessionId(UUID.randomUUID().toString())
                .build();
        CurrentUserAuthenticationToken auth = new CurrentUserAuthenticationToken(
                username, "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), userData
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void approvePendingRequests(MakerCheckerEntityName entityName, Object adapter) {
        authenticate("admin_checker", CHECKER_ID);
        List<MakerCheckerRequestEntity> pending = requestRepo.findAll().stream()
                .filter(r -> r.getEntityName() == entityName && r.getStatus() == MakerCheckerStatus.PENDING)
                .toList();

        for (MakerCheckerRequestEntity req : pending) {
            try {
                ValidateOrRejectSubmissionInput validationInput = new ValidateOrRejectSubmissionInput(
                        req.getId(), MakerCheckerStatus.ACCEPTED, "Approbation automatique de test", req.getCheckerOperationType()
                );
                
                if (adapter instanceof JournalAdapter) {
                    ((JournalAdapter) adapter).validateOrReject(validationInput);
                } else if (adapter instanceof BankAdapter) {
                    ((BankAdapter) adapter).validateOrReject(validationInput);
                } else if (adapter instanceof TiersAdapter) {
                    ((TiersAdapter) adapter).validateOrReject(validationInput);
                } else if (adapter instanceof ImmobilisationAdapter) {
                    ((ImmobilisationAdapter) adapter).validateOrReject(validationInput);
                } else if (adapter instanceof UserAdapter) {
                    ((UserAdapter) adapter).validateOrReject(validationInput);
                }
            } catch (Exception e) {
                log.warn("Impossible d'approuver la requête Maker-Checker {} : {}", req.getId(), e.getMessage());
            }
        }
        authenticate("admin_maker", MAKER_ID);
    }

    private void seedRefData() {
        log.info("  ↳ Données de référence (Journaux, Banques, Tiers, Users, Immo, Analytique)...");
        authenticate("admin_maker", MAKER_ID);

        // 1. Journaux
        for (CodeJournal code : CodeJournal.values()) {
            if (journalRepository.findByCode(code).isPresent()) continue;
            try {
                journalAdapter.initCreateJournal(CreateJournalInput.builder()
                        .code(code)
                        .intitule("Journal des " + code.getDescription())
                        .typeJournal("DIVERS")
                        .build());
            } catch (Exception e) {
                log.error("Échec de création du journal " + code, e);
            }
        }
        approvePendingRequests(MakerCheckerEntityName.JOURNAL, journalAdapter);
        journalRepository.findAll().forEach(j -> journalIds.put(j.getCode().name(), j.getId()));

        // 2. Banques (avec logos téléchargés)
        Object[][] banks = {
            {"AFB", "Afriland First Bank", "521", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRfStUncJi6M6LEadtwnXw6jYy2aNssdl4TPTHr4oE9Pg&s=10"},
            {"SGC", "Société Générale Cameroun", "522", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQbRpJVLtz-pYv26LnHMp9MNyx7PRglJmMMYhvwLoxItA&s=10"},
            {"BICEC", "BICEC", "523", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQObxQAhL3gs41eD4uHVT-sH1MujkDzVV7vgYSyh98iFQ&s=10"},
            {"CBC", "Commercial Bank of Cameroon", "524", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQgpOLwLzUf8vC9cN2fW_czKAlvi9RfbJuiDuXRCLgVJQ&s=10"},
            {"ECO", "Ecobank Cameroun", "525", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQrymqgneRE1OOQf5QLhihQnM3bSKfVmy_wl5SMv3fFEQ&s=10"},
            {"UBA", "UBA Cameroun", "526", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTIpO4KursaiwAzpIYskhhaV3ZpETftfNf7Wa4sz07XhQ&s=10"}
        };
        for (Object[] b : banks) {
            if (banqueRepository.findByCode((String) b[0]).isPresent()) continue;
            try {
                byte[] logoBytes = downloadBytes((String) b[3]);
                DummyMultipartFile logoFile = new DummyMultipartFile("logo", "logo.png", "image/png", logoBytes);
                bankAdapter.initCreateBank(new CreateBankInput((String) b[0], (String) b[1], (String) b[2], logoFile));
            } catch (Exception e) {
                log.warn("Échec flow normal Banque {}, repli direct...", b[0]);
                banqueRepository.save(BanqueEntity.builder()
                        .id(UUID.randomUUID()).code((String) b[0]).nom((String) b[1]).compteComptableCode((String) b[2]).logo((String) b[3]).status(true).build());
            }
        }
        approvePendingRequests(MakerCheckerEntityName.BANQUE, bankAdapter);
        banqueRepository.findAll().forEach(b -> bankIds.put(b.getCode(), b.getId()));

        // 3. Tiers (partenaires réels de la SODEPA)
        Object[][] partners = {
            // Clients
            {"CLI001", "Carrefour Cameroun (CFAO Retail)", "Playce Yaoundé", "+237 222 00 11 22", "retail@carrefour.cm", TypeTiers.CLIENT, "411"},
            {"CLI002", "Supermarchés Casino Douala", "Akwa Douala", "+237 233 44 55 66", "achats@casino.cm", TypeTiers.CLIENT, "411"},
            {"CLI003", "Congelcam S.A.", "Zone Industrielle Bassa", "+237 233 55 66 77", "congelcam@congelcam.cm", TypeTiers.CLIENT, "411"},
            {"CLI004", "Boucherie Centrale Yaoundé", "Marché Central", "+237 222 11 22 33", "centrale@boucherie.cm", TypeTiers.CLIENT, "411"},
            // Fournisseurs
            {"FOU001", "MTN Cameroon", "Boulevard de la Liberté", "+237 679 00 00 00", "entreprise@mtn.cm", TypeTiers.FOURNISSEUR, "401"},
            {"FOU002", "CAMRAIL (AGL)", "Gare de Bessengue", "+237 233 30 30 30", "logistique@camrail.cm", TypeTiers.FOURNISSEUR, "401"},
            {"FOU003", "CFAO Motors Cameroun", "Avenue de Gaulle", "+237 233 40 40 40", "auto@cfao.cm", TypeTiers.FOURNISSEUR, "401"},
            {"FOU004", "LANAVET (Garoua)", "Laboratoire Vétérinaire", "+237 222 27 12 85", "lanavet@lanavet.cm", TypeTiers.FOURNISSEUR, "401"},
            {"FOU005", "SODECOTON", "Garoua Centre", "+237 222 27 10 77", "sodecoton@sodecoton.cm", TypeTiers.FOURNISSEUR, "401"},
            {"FOU006", "Provenderie du Cameroun S.A.", "Bonabéri Douala", "+237 233 39 12 34", "spc@spc.cm", TypeTiers.FOURNISSEUR, "401"},
            {"FOU007", "CAMVET S.A.", "Pharmacie Vétérinaire Yaoundé", "+237 222 21 44 55", "camvet@camvet.cm", TypeTiers.FOURNISSEUR, "401"},
            {"FOU008", "CENEEMA", "Yaoundé Mvan", "+237 222 30 15 15", "ceneema@ceneema.cm", TypeTiers.FOURNISSEUR, "401"}
        };
        for (Object[] p : partners) {
            if (tiersRepository.findByCode((String) p[0]).isPresent()) continue;
            try {
                tiersAdapter.initCreateTiers(new CreateTiersInput(
                        (String) p[0], (String) p[1], (String) p[2], (String) p[3], (String) p[4], (TypeTiers) p[5], (String) p[6]
                ));
            } catch (Exception e) {
                log.warn("Échec flow normal Tiers {}, repli direct...", p[0]);
                tiersRepository.save(TiersEntity.builder()
                        .id(UUID.randomUUID()).code((String) p[0]).raisonSociale((String) p[1]).adresse((String) p[2])
                        .telephone((String) p[3]).email((String) p[4]).typeTiers((TypeTiers) p[5]).compteCollectifCode((String) p[6]).actif(true).build());
            }
        }
        approvePendingRequests(MakerCheckerEntityName.TIERS, tiersAdapter);
        tiersRepository.findAll().forEach(t -> tiersIds.put(t.getCode(), t.getId()));

        // 4. 100 Utilisateurs avec différentes permissions
        for (int i = 1; i <= 100; i++) {
            String nom = CAMEROON_LASTNAMES[i - 1];
            String prenom = CAMEROON_FIRSTNAMES[i - 1];
            String username = Normalizer
                    .normalize(nom, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .replaceAll("[^a-zA-Z0-9]", "")+"_"+
                    Normalizer
                            .normalize(prenom, Normalizer.Form.NFD)
                            .replaceAll("\\p{M}", "")
                            .replaceAll("[^a-zA-Z0-9]", "");
            String email = username + "@sodepa.cm";
            Set<Permissions> perms = new HashSet<>();
            if (i <= 10) { // Admins
                perms.addAll(Arrays.asList(Permissions.values()));
            } else if (i <= 50) { // Makers
                for (Permissions p : Permissions.values()) {
                    if (p.name().startsWith("INIT_") || p.name().startsWith("GET_")) perms.add(p);
                }
            } else if (i <= 90) { // Checkers
                for (Permissions p : Permissions.values()) {
                    if (p.name().startsWith("VALIDATE_") || p.name().startsWith("GET_")) perms.add(p);
                }
            } else { // Consultants
                for (Permissions p : Permissions.values()) {
                    if (p.name().startsWith("GET_")) perms.add(p);
                }
            }

            Optional<UtilisateurEntity> existing = userRepository.findByUsername(username);
            if (existing.isPresent()) {
                UtilisateurEntity user = existing.get();
                if (!nom.equals(user.getNom()) || !prenom.equals(user.getPrenom())) {
                    user.setNom(nom);
                    user.setPrenom(prenom);
                    userRepository.save(user);
                    log.info("Mise à jour de l'utilisateur {} avec le nom réel : {} {}", username, prenom, nom);
                }
                continue;
            }

            try {
                userAdapter.initCreateUser(new CreateUserInput(
                        username, nom, prenom, email, Set.of("+23769" + String.format("%07d", i)), perms,
                        new DummyMultipartFile("photo", "photo.png", "image/png", new byte[0])
                ));
            } catch (Exception e) {
                // Repli direct en base
                userRepository.save(UtilisateurEntity.builder()
                        .id(UUID.randomUUID()).iam(UUID.randomUUID()).username(username).nom(nom).prenom(prenom)
                        .email(email).telephones(Set.of("+23769" + String.format("%07d", i))).permissions(perms).actif(true).build());
            }
        }
        approvePendingRequests(MakerCheckerEntityName.USER, userAdapter);

        // 5. Immobilisations
        Object[][] immos = {
            {"IMM-001", "Camion frigorifique ISUZU NPR", 45_000_000, LocalDate.of(2020, 3, 15), ModeAmortissement.LINEAIRE, 8},
            {"IMM-002", "Bâtiment d'élevage Ngaoundéré", 120_000_000, LocalDate.of(2018, 1, 10), ModeAmortissement.LINEAIRE, 20},
            {"IMM-003", "Tracteur John Deere 5045D", 28_000_000, LocalDate.of(2021, 7, 20), ModeAmortissement.DEGRESSIF, 10},
            {"IMM-004", "Groupe électrogène Caterpillar 100KVA", 15_000_000, LocalDate.of(2022, 2, 1), ModeAmortissement.LINEAIRE, 10},
            {"IMM-005", "Véhicule Toyota Hilux double cabine", 22_000_000, LocalDate.of(2017, 5, 10), ModeAmortissement.LINEAIRE, 5}
        };
        for (Object[] im : immos) {
            if (immobilisationRepository.findByCode((String) im[0]).isPresent()) continue;
            try {
                immobilisationAdapter.initCreateImmo(new CreateImmoInput(
                        (String) im[0], (String) im[1], BigDecimal.valueOf((int) im[2]), (LocalDate) im[3], (LocalDate) im[3],
                        (ModeAmortissement) im[4], (int) im[5], BigDecimal.ZERO
                ));
            } catch (Exception e) {
                log.warn("Échec flow normal Immobilisation {}, repli direct...", im[0]);
                immobilisationRepository.save(ImmobilisationEntity.builder()
                        .id(UUID.randomUUID()).code((String) im[0]).designation((String) im[1]).valeurOrigine(BigDecimal.valueOf((int) im[2]))
                        .dateAcquisition((LocalDate) im[3]).dateMiseEnService((LocalDate) im[3]).modeAmortissement((ModeAmortissement) im[4])
                        .dureeUtile((int) im[5]).valeurResiduelle(BigDecimal.ZERO).statut(StatutImmobilisation.ACTIVE).build());
            }
        }
        approvePendingRequests(MakerCheckerEntityName.IMMOBILISATION, immobilisationAdapter);

        // 6. Axes & Sections analytiques
        if (!axeRepository.existsByCode("RANCH")) {
            AxeAnalytiqueEntity axeRanch = axeRepository.save(AxeAnalytiqueEntity.builder().code("RANCH").intitule("Ranches et Sites d'élevage").actif(true).build());
            AxeAnalytiqueEntity axeActivite = axeRepository.save(AxeAnalytiqueEntity.builder().code("ACTIVITE").intitule("Activités et Filières").actif(true).build());

            secNgd = sectionRepository.save(SectionAnalytiqueEntity.builder().axe(axeRanch).code("SEC-NGD").intitule("Ranch de Ngaoundéré").actif(true).build());
            secDla = sectionRepository.save(SectionAnalytiqueEntity.builder().axe(axeRanch).code("SEC-DLA").intitule("Centre de Douala").actif(true).build());
            secYde = sectionRepository.save(SectionAnalytiqueEntity.builder().axe(axeRanch).code("SEC-YDE").intitule("Direction Yaoundé").actif(true).build());

            secBov = sectionRepository.save(SectionAnalytiqueEntity.builder().axe(axeActivite).code("SEC-BOV").intitule("Filière Bovine").actif(true).build());
            secOvi = sectionRepository.save(SectionAnalytiqueEntity.builder().axe(axeActivite).code("SEC-OVI").intitule("Filière Ovine").actif(true).build());
            secAvi = sectionRepository.save(SectionAnalytiqueEntity.builder().axe(axeActivite).code("SEC-AVI").intitule("Filière Avicole").actif(true).build());

            // Clé de répartition
            CleRepartitionEntity cleRanch = CleRepartitionEntity.builder().code("REP_RANCH").intitule("Répartition par site").actif(true).build();
            cleRanch.addDetail(DetailCleRepartitionEntity.builder().section(secNgd).pourcentage(BigDecimal.valueOf(50)).build());
            cleRanch.addDetail(DetailCleRepartitionEntity.builder().section(secDla).pourcentage(BigDecimal.valueOf(30)).build());
            cleRanch.addDetail(DetailCleRepartitionEntity.builder().section(secYde).pourcentage(BigDecimal.valueOf(20)).build());
            cleRepartitionRepository.save(cleRanch);
        } else {
            List<SectionAnalytiqueEntity> allSections = sectionRepository.findAll();
            secNgd = allSections.stream().filter(s -> s.getCode().equals("SEC-NGD")).findFirst().orElse(null);
            secDla = allSections.stream().filter(s -> s.getCode().equals("SEC-DLA")).findFirst().orElse(null);
            secYde = allSections.stream().filter(s -> s.getCode().equals("SEC-YDE")).findFirst().orElse(null);
        }
    }

    private void seedHistoricalData() {
        log.info("  ↳ Production de l'historique financier et budgétaire sur 10 ans (2017-2026)...");

        List<String> realClients = List.of("CLI001", "CLI002", "CLI003", "CLI004");
        List<String> realSuppliers = List.of("FOU005", "FOU006", "FOU007", "FOU008");

        for (int annee = 2017; annee <= 2026; annee++) {
            // A. Plans budgétaires annuels (Maker-Checker Use Cases)
            if (budgetPlanRepository.findByAnnee(annee).isEmpty()) {
                try {
                    authenticate("admin_maker", MAKER_ID);
                    creerBudgetPlanUseCase.execute(new CreerBudgetPlanInput(annee, "Budget Annuel SODEPA " + annee, MAKER_ID));
                    
                    BudgetPlanEntity plan = budgetPlanRepository.findByAnnee(annee).get(0);
                    UUID planId = plan.getId();

                    // Ajouter les items budgétaires
                    if (secNgd != null) {
                        ajouterItemPlanUseCase.execute(new AjouterItemInput(planId, "601", secNgd.getId(), BigDecimal.valueOf(200_000_000)));
                        ajouterItemPlanUseCase.execute(new AjouterItemInput(planId, "601", secDla.getId(), BigDecimal.valueOf(150_000_000)));
                    }
                    ajouterItemPlanUseCase.execute(new AjouterItemInput(planId, "661", null, BigDecimal.valueOf(300_000_000)));
                    ajouterItemPlanUseCase.execute(new AjouterItemInput(planId, "701", null, BigDecimal.valueOf(200_000_000)));

                    // Valider le plan
                    soumettrePlanUseCase.execute(new SoumettrePlanUseCase.Input(planId, MAKER_ID));
                    authenticate("admin_checker", CHECKER_ID);
                    approuverPlanUseCase.execute(new ApprouverPlanUseCase.Input(planId, CHECKER_ID));
                } catch (Exception e) {
                    log.error("Échec création budget " + annee + " : " + e.getMessage());
                }
            }

            // B. Saisie & Validation d'Écritures Comptables (Utilise Saisir & Valider UseCases)
            int finalAnnee = annee;
            UUID journalHAId = journalIds.get("HA");
            UUID journalVTId = journalIds.get("VT");
            UUID journalBQId = journalIds.get("BQ");

            int maxMonths = (annee == 2026) ? 8 : 12;

            for (int m = 1; m <= maxMonths; m++) {
                final int month = m;

                // 1. Transaction d'Achat mensuelle (Charge 601)
                try {
                    authenticate("admin_maker", MAKER_ID);
                    String clientCode = realSuppliers.get((month + finalAnnee) % realSuppliers.size());
                    UUID supId = tiersIds.get(clientCode);
                    BigDecimal rawAmount = BigDecimal.valueOf(5_000_000 + month * 100_000);
                    BigDecimal tvaAmount = rawAmount.multiply(BigDecimal.valueOf(0.1925)).setScale(0, RoundingMode.HALF_UP);
                    BigDecimal totalAmount = rawAmount.add(tvaAmount);

                    List<LigneInput> lignes = List.of(
                            new LigneInput("601", null, rawAmount, BigDecimal.ZERO, "Achat aliments bétail"),
                            new LigneInput("445", null, tvaAmount, BigDecimal.ZERO, "TVA déductible"),
                            new LigneInput("401", supId, BigDecimal.ZERO, totalAmount, "Fournisseur intrants")
                    );

                    String numPiece = String.format("HA-%d-%02d", finalAnnee, month);
                    saisirEcritureUseCase.execute(new SaisieEcritureInput(
                            journalHAId, numPiece, "Facture d'achat mensuelle M" + month,
                            LocalDate.of(finalAnnee, month, 10), Devise.XAF, BigDecimal.ONE, lignes
                    ));

                    // Validation
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        Optional<EcritureEntity> created = ecritureRepository.findAll().stream()
                                .filter(e -> numPiece.equals(e.getNumeroPiece())).findFirst();
                        if (created.isPresent()) {
                            Optional<CleRepartitionEntity> cleOpt = cleRepartitionRepository.findByCode("REP_RANCH");
                            if (cleOpt.isPresent()) {
                                UUID cleRanchId = cleOpt.get().getId();
                                for (LigneEcritureEntity ligne : created.get().getLignes()) {
                                    if (ligne.getCompteCode().startsWith("6") || ligne.getCompteCode().startsWith("7")) {
                                        cleRepartitionUseCase.appliquerCle(ligne.getId(), cleRanchId);
                                    }
                                }
                            }
                            soumettreEcritureUseCase.execute(created.get().getId());
                            authenticate("admin_checker", CHECKER_ID);
                            validerEcritureUseCase.execute(created.get().getId());
                        }
                    });
                } catch (Exception e) {
                    log.debug("Erreur écriture HA: {}", e.getMessage());
                }

                // 2. Transaction de Vente mensuelle (Produit 701)
                try {
                    authenticate("admin_maker", MAKER_ID);
                    String clientCode = realClients.get((month + finalAnnee) % realClients.size());
                    UUID cliId = tiersIds.get(clientCode);
                    BigDecimal rawAmount = BigDecimal.valueOf(7_500_000 + month * 150_000);
                    BigDecimal tvaAmount = rawAmount.multiply(BigDecimal.valueOf(0.1925)).setScale(0, RoundingMode.HALF_UP);
                    BigDecimal totalAmount = rawAmount.add(tvaAmount);

                    List<LigneInput> lignes = List.of(
                            new LigneInput("411", cliId, totalAmount, BigDecimal.ZERO, "Client viande"),
                            new LigneInput("701", null, BigDecimal.ZERO, rawAmount, "Vente production bovine"),
                            new LigneInput("443", null, BigDecimal.ZERO, tvaAmount, "TVA collectée")
                    );

                    String numPiece = String.format("VT-%d-%02d", finalAnnee, month);
                    saisirEcritureUseCase.execute(new SaisieEcritureInput(
                            journalVTId, numPiece, "Facture de vente mensuelle M" + month,
                            LocalDate.of(finalAnnee, month, 15), Devise.XAF, BigDecimal.ONE, lignes
                    ));

                    // Validation
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        Optional<EcritureEntity> created = ecritureRepository.findAll().stream()
                                .filter(e -> numPiece.equals(e.getNumeroPiece())).findFirst();
                        if (created.isPresent()) {
                            Optional<CleRepartitionEntity> cleOpt = cleRepartitionRepository.findByCode("REP_RANCH");
                            if (cleOpt.isPresent()) {
                                UUID cleRanchId = cleOpt.get().getId();
                                for (LigneEcritureEntity ligne : created.get().getLignes()) {
                                    if (ligne.getCompteCode().startsWith("6") || ligne.getCompteCode().startsWith("7")) {
                                        cleRepartitionUseCase.appliquerCle(ligne.getId(), cleRanchId);
                                    }
                                }
                            }
                            soumettreEcritureUseCase.execute(created.get().getId());
                            authenticate("admin_checker", CHECKER_ID);
                            validerEcritureUseCase.execute(created.get().getId());
                        }
                    });
                } catch (Exception e) {
                    log.debug("Erreur écriture VT: {}", e.getMessage());
                }

                // 3. Transaction de Paie mensuelle (Salaires 661)
                try {
                    authenticate("admin_maker", MAKER_ID);
                    BigDecimal totalSalaires = BigDecimal.valueOf(12_000_000 + (finalAnnee - 2017) * 500_000);

                    List<LigneInput> lignes = List.of(
                            new LigneInput("661", null, totalSalaires, BigDecimal.ZERO, "Rémunération personnel"),
                            new LigneInput("521", null, BigDecimal.ZERO, totalSalaires, "Virement Afriland")
                    );

                    String numPiece = String.format("BQ-%d-%02d", finalAnnee, month);
                    saisirEcritureUseCase.execute(new SaisieEcritureInput(
                            journalBQId, numPiece, "Paiement salaires mensuels M" + month,
                            LocalDate.of(finalAnnee, month, 25), Devise.XAF, BigDecimal.ONE, lignes
                    ));

                    // Validation
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        Optional<EcritureEntity> created = ecritureRepository.findAll().stream()
                                .filter(e -> numPiece.equals(e.getNumeroPiece())).findFirst();
                        if (created.isPresent()) {
                            Optional<CleRepartitionEntity> cleOpt = cleRepartitionRepository.findByCode("REP_RANCH");
                            if (cleOpt.isPresent()) {
                                UUID cleRanchId = cleOpt.get().getId();
                                for (LigneEcritureEntity ligne : created.get().getLignes()) {
                                    if (ligne.getCompteCode().startsWith("6") || ligne.getCompteCode().startsWith("7")) {
                                        cleRepartitionUseCase.appliquerCle(ligne.getId(), cleRanchId);
                                    }
                                }
                            }
                            soumettreEcritureUseCase.execute(created.get().getId());
                            authenticate("admin_checker", CHECKER_ID);
                            validerEcritureUseCase.execute(created.get().getId());
                        }
                    });
                } catch (Exception e) {
                    log.debug("Erreur écriture BQ: {}", e.getMessage());
                }
            }

            // C. Budgets analytiques (Comptabilité Analytique)
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    authenticate("admin_maker", MAKER_ID);
                    if (secNgd != null) {
                        BudgetUseCase.BudgetRequest req1 = new BudgetUseCase.BudgetRequest();
                        req1.setAnnee(finalAnnee);
                        req1.setSectionId(secNgd.getId());
                        req1.setCompteCode("601");
                        req1.setMontantBudget(BigDecimal.valueOf(200_000_000));
                        budgetUseCase.definirBudget(req1);
                    }
                    if (secDla != null) {
                        BudgetUseCase.BudgetRequest req2 = new BudgetUseCase.BudgetRequest();
                        req2.setAnnee(finalAnnee);
                        req2.setSectionId(secDla.getId());
                        req2.setCompteCode("601");
                        req2.setMontantBudget(BigDecimal.valueOf(150_000_000));
                        budgetUseCase.definirBudget(req2);
                    }
                });
            } catch (Exception e) {
                log.debug("Erreur budget analytique {}: {}", finalAnnee, e.getMessage());
            }

            // D. Dotations aux Amortissements
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    authenticate("admin_maker", MAKER_ID);
                    immobilisationAdapter.initGenerateAmortisation(new GenerateAmortisationInput(finalAnnee, "240000"));
                    Optional<MakerCheckerRequestEntity> pendingImmo = requestRepo.findAll().stream()
                            .filter(r -> r.getEntityName() == MakerCheckerEntityName.IMMOBILISATION 
                                      && r.getStatus() == MakerCheckerStatus.PENDING 
                                      && r.getEntityPk().startsWith("SYSTEM-"))
                            .findFirst();
                    if (pendingImmo.isPresent()) {
                        authenticate("admin_checker", CHECKER_ID);
                        immobilisationAdapter.validateOrReject(new ValidateOrRejectSubmissionInput(
                                pendingImmo.get().getId(), MakerCheckerStatus.ACCEPTED, "Amortissement annuel " + finalAnnee, MakerCheckerOperationType.UPDATE
                        ));
                    }
                });
            } catch (Exception e) {
                log.debug("Erreur amortissement annuel {}: {}", finalAnnee, e.getMessage());
            }

            // E. Clôture Comptable de l'Exercice
            if (annee < 2026) {
                try {
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        authenticate("admin_maker", MAKER_ID);
                        fiscalYearClosingAdapter.fiscalYearClosing(finalAnnee);
                        Optional<MakerCheckerRequestEntity> pendingClosing = requestRepo.findAll().stream()
                                .filter(r -> r.getEntityName() == MakerCheckerEntityName.FISCALYEARCLOSING 
                                          && r.getStatus() == MakerCheckerStatus.PENDING)
                                .findFirst();
                        if (pendingClosing.isPresent()) {
                            authenticate("admin_checker", CHECKER_ID);
                            fiscalYearClosingAdapter.generateFiscalYearClosingSimulation(new ValidateOrRejectSubmissionInput(
                                    pendingClosing.get().getId(), MakerCheckerStatus.ACCEPTED, "Clôture exercice " + finalAnnee, MakerCheckerOperationType.CREATE
                            ));
                        }
                    });
                } catch (Exception e) {
                    log.debug("Erreur clôture comptable {}: {}", finalAnnee, e.getMessage());
                }
            }
        }
    }

    private void seedTreasuryData() {
        log.info("  ↳ Trésorerie, Financements et Couvertures (Exercice 2026)...");
        authenticate("admin_maker", MAKER_ID);

        UUID afrilandId = bankIds.get("AFB");
        UUID sgcId = bankIds.get("SGC");
        UUID provenderieId = tiersIds.get("FOU006");
        UUID cfaoId = tiersIds.get("FOU003");

        // 1. Prévisions de trésorerie (Use Cases)
        try {
            ajouterPrevisionUseCase.execute(new CreerPrevisionInput(LocalDate.of(2026, 9, 15), "ENCAISSEMENT", "CLIENT", "Vente viande bovine", BigDecimal.valueOf(15_000_000)));
            ajouterPrevisionUseCase.execute(new CreerPrevisionInput(LocalDate.of(2026, 10, 10), "ENCAISSEMENT", "CLIENT", "Règlement Casino Douala", BigDecimal.valueOf(8_500_000)));
            ajouterPrevisionUseCase.execute(new CreerPrevisionInput(LocalDate.of(2026, 9, 28), "DECAISSEMENT", "SALAIRE", "Salaires personnel sept 2026", BigDecimal.valueOf(12_000_000)));
            ajouterPrevisionUseCase.execute(new CreerPrevisionInput(LocalDate.of(2026, 10, 5), "DECAISSEMENT", "FOURNISSEUR", "Provenderie du Cameroun S.A.", BigDecimal.valueOf(25_000_000)));
        } catch (Exception e) {}

        // 2. Financements (Emprunts)
        try {
            if (afrilandId != null) {
                enregistrerFinancementUseCase.execute(new CreerFinancementInput(
                        afrilandId, "Prêt équipement Ranch Ngaoundéré", "PRET", BigDecimal.valueOf(100_000_000),
                        BigDecimal.valueOf(6.50), LocalDate.of(2025, 1, 15), 60, "RESTANT", MAKER_ID
                ));
            }
            if (sgcId != null) {
                enregistrerFinancementUseCase.execute(new CreerFinancementInput(
                        sgcId, "Crédit-bail camion frigorifique", "LEASING", BigDecimal.valueOf(45_000_000),
                        BigDecimal.valueOf(8.00), LocalDate.of(2024, 6, 1), 48, "RESTANT", MAKER_ID
                ));
            }
        } catch (Exception e) {}

        // 3. Découverts bancaires
        try {
            if (afrilandId != null && ligneDecouvertRepository.findByBanqueId(afrilandId).isEmpty()) {
                ligneDecouvertRepository.save(LigneDecouvertEntity.builder()
                        .banqueId(afrilandId).intitule("Facilité découvert Afriland").plafond(BigDecimal.valueOf(50_000_000))
                        .tauxInteret(BigDecimal.valueOf(12.00)).soldeUtilise(BigDecimal.valueOf(15_000_000))
                        .dateDebut(LocalDate.of(2026, 1, 1)).dateFin(LocalDate.of(2026, 12, 31)).alerteSeuilPourcent(BigDecimal.valueOf(80.00)).build());
            }
        } catch (Exception e) {}

        // 4. Contrats de couverture (Use Cases)
        try {
            enregistrerCouvertureUseCase.execute(new CouvertureInput("FWD-2026-001", "EUR", BigDecimal.valueOf(150_000), BigDecimal.valueOf(655.957), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 9, 30)));
        } catch (Exception e) {}

        // 5. Engagements hors-bilan (Use Cases)
        try {
            if (provenderieId != null) {
                enregistrerEngagementHorsBilanUseCase.execute(new CreerHorsBilanInput("CAUTIONNEMENT", "Caution bancaire Afriland - Marché MINADER", provenderieId, BigDecimal.valueOf(25_000_000), LocalDate.of(2026, 1, 15), LocalDate.of(2027, 1, 14)));
            }
            if (cfaoId != null) {
                enregistrerEngagementHorsBilanUseCase.execute(new CreerHorsBilanInput("GARANTIE_BANCAIRE", "Garantie CFAO Motors", cfaoId, BigDecimal.valueOf(15_000_000), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 12, 31)));
            }
        } catch (Exception e) {}

        // 6. Relevé bancaire Afriland (Comptabilité générale)
        try {
            if (afrilandId != null && releveBancaireRepository.count() == 0) {
                BanqueEntity bank = banqueRepository.findById(afrilandId).orElseThrow();
                ReleveBancaireEntity releve = ReleveBancaireEntity.builder()
                        .id(UUID.randomUUID()).banque(bank).dateReleve(LocalDate.of(2026, 8, 31))
                        .soldeInitial(BigDecimal.valueOf(45_000_000)).soldeFinal(BigDecimal.valueOf(38_500_000)).valide(false).build();

                releve.addLigne(LigneReleveBancaireEntity.builder().id(UUID.randomUUID()).dateTransaction(LocalDate.of(2026, 8, 5)).libelle("Encaissement client - Boucherie Centrale Yaoundé").montant(BigDecimal.valueOf(8_925_000)).rapproche(false).build());
                releve.addLigne(LigneReleveBancaireEntity.builder().id(UUID.randomUUID()).dateTransaction(LocalDate.of(2026, 8, 25)).libelle("Virement salaires personnel août 2026").montant(BigDecimal.valueOf(-12_000_000)).rapproche(false).build());
                releve.addLigne(LigneReleveBancaireEntity.builder().id(UUID.randomUUID()).dateTransaction(LocalDate.of(2026, 8, 12)).libelle("Paiement fournisseur - Provenderie du Cameroun").montant(BigDecimal.valueOf(-5_962_500)).rapproche(false).build());
                releve.addLigne(LigneReleveBancaireEntity.builder().id(UUID.randomUUID()).dateTransaction(LocalDate.of(2026, 8, 31)).libelle("Frais bancaires et commissions août").montant(BigDecimal.valueOf(-25_000)).rapproche(false).build());
                releve.addLigne(LigneReleveBancaireEntity.builder().id(UUID.randomUUID()).dateTransaction(LocalDate.of(2026, 8, 20)).libelle("Encaissement client - Supermarché Casino Douala").montant(BigDecimal.valueOf(2_562_500)).rapproche(false).build());

                releveBancaireRepository.save(releve);
            }
        } catch (Exception e) {}
    }

    private byte[] downloadBytes(String urlString) {
        try (InputStream in = new URL(urlString).openStream()) {
            return in.readAllBytes();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static class DummyMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public DummyMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content == null || content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() throws IOException { return content; }
        @Override public InputStream getInputStream() throws IOException { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws IOException, IllegalStateException {}
    }
}
