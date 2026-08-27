package com.sodepa.erp.comptabilite.generale.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.LigneReleveInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.RapprochementInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.RechercheReleveInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ReleveManuelInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.SyncInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.ReleveBancaireOutput;
import com.sodepa.erp.comptabilite.generale.application.usecase.EffectuerRapprochementAutomatiqueUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.GetPageRelevesUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.GetReleveByIdUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.SaisirReleveManuelUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.SynchroniserReleveAutomatiqueUseCase;
import com.sodepa.erp.comptabilite.generale.presentation.requests.ReleveManuelRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.SyncRequest;
import com.sodepa.erp.utils.PageRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comptabilite/rapprochement")
@RequiredArgsConstructor
public class RapprochementRestController {

    private final SaisirReleveManuelUseCase saisirReleveManuelUseCase;
    private final SynchroniserReleveAutomatiqueUseCase synchroniserReleveAutomatiqueUseCase;
    private final EffectuerRapprochementAutomatiqueUseCase effectuerRapprochementAutomatiqueUseCase;
    private final GetPageRelevesUseCase getPageRelevesUseCase;
    private final GetReleveByIdUseCase getReleveByIdUseCase;

    /**
     * Liste les relevés importés.
     *
     * @param banqueId restreint à une banque, facultatif
     * @param valide restreint aux relevés rapprochés ou non, facultatif
     */
    @GetMapping("/releves")
    public PageRecord<ReleveBancaireOutput> listerReleves(
            @PageableDefault Pageable pageable,
            @RequestParam(required = false) UUID banqueId,
            @RequestParam(required = false) Boolean valide
    ) {
        return getPageRelevesUseCase.execute(new RechercheReleveInput(pageable, banqueId, valide));
    }

    /**
     * Consulte un relevé et ses lignes.
     */
    @GetMapping("/releves/{releveId}")
    public ReleveBancaireOutput getReleve(@PathVariable UUID releveId) {
        return getReleveByIdUseCase.execute(releveId);
    }

    @PostMapping("/manuel")
    public ReleveBancaireOutput saisirReleveManuel(@RequestBody @Valid ReleveManuelRequest request) {
        ReleveManuelInput input = new ReleveManuelInput(
                request.banqueId(),
                request.dateReleve(),
                request.soldeInitial(),
                request.soldeFinal(),
                request.lignes().stream()
                        .map(l -> new LigneReleveInput(l.dateTransaction(), l.libelle(), l.montant()))
                        .collect(Collectors.toList())
        );
        return saisirReleveManuelUseCase.execute(input);
    }

    @PostMapping("/synchroniser")
    public ReleveBancaireOutput synchroniserReleveAutomatique(@RequestBody @Valid SyncRequest request) {
        SyncInput input = new SyncInput(
                request.banqueId(),
                request.dateReleve(),
                request.soldeInitial()
        );
        return synchroniserReleveAutomatiqueUseCase.execute(input);
    }

    @PostMapping("/{releveId}/rapprocher")
    public Integer effectuerRapprochementAutomatique(
            @PathVariable UUID releveId,
            @RequestParam String compteBanqueCode) {
        RapprochementInput input = new RapprochementInput(releveId, compteBanqueCode);
        return effectuerRapprochementAutomatiqueUseCase.execute(input);
    }
}
