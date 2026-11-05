package com.sodepa.erp.organigramme.application.ports;

import com.sodepa.erp.comptabilite.generale.application.inputs.ValidateOrRejectSubmissionInput;
import com.sodepa.erp.organigramme.application.inputs.*;
import com.sodepa.erp.organigramme.application.outputs.*;

import java.util.List;
import java.util.UUID;

public interface OrganigrammePort {

    UniteOrganisationnelleOutput creerUnite(CreerUniteInput input);

    UniteOrganisationnelleOutput deplacerUnite(DeplacerUniteInput input);

    void validateOrRejectUnite(ValidateOrRejectSubmissionInput input);

    List<UniteOrganisationnelleOutput> listerUnites();

    List<ArborescenceNoeudOutput> getArborescenceComplete();

    ArborescenceNoeudOutput getSousArbreUnite(UUID uniteId);

    PosteOutput creerPoste(CreerPosteInput input);

    void validateOrRejectPoste(ValidateOrRejectSubmissionInput input);

    List<PosteOutput> listerPostes(UUID uniteId);

    AffectationOutput affecterCollaborateur(AffecterCollaborateurInput input);

    AffectationOutput cloturerAffectation(CloturerAffectationInput input);

    void validateOrRejectAffectation(ValidateOrRejectSubmissionInput input);

    DelegationOutput creerDelegation(CreerDelegationInput input);

    void validateOrRejectDelegation(ValidateOrRejectSubmissionInput input);

    List<DelegationOutput> listerDelegationsActives();

    OrganigrammeVersionOutput creerVersion(CreerVersionOrganigrammeInput input);

    OrganigrammeVersionOutput activerVersion(UUID versionId, UUID approuvePar);

    List<OrganigrammeVersionOutput> listerVersions();

    // ==========================================
    // TYPES DE STRUCTURES
    // ==========================================
    TypeStructureOutput creerTypeStructure(CreerTypeStructureInput input);

    TypeStructureOutput modifierTypeStructure(ModifierTypeStructureInput input);

    void supprimerTypeStructure(UUID id);

    List<TypeStructureOutput> listerTypesStructures();

    TypeStructureOutput getTypeStructure(UUID id);

    // ==========================================
    // OBJECTIFS D'UNITÉS
    // ==========================================
    ObjectifUniteOutput ajouterObjectif(CreerObjectifInput input);

    ObjectifUniteOutput mettreAJourRealiseObjectif(MettreAJourRealiseObjectifInput input);

    void supprimerObjectif(UUID id);

    List<ObjectifUniteOutput> listerObjectifsUnite(UUID uniteId, Integer anneeExercice);
}
