package com.sodepa.erp.comptabilite.generale.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.LigneReleveInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.RapprochementInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.ReleveManuelInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.SyncInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.ReleveBancaireOutput;
import com.sodepa.erp.comptabilite.generale.application.usecase.EffectuerRapprochementAutomatiqueUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.SaisirReleveManuelUseCase;
import com.sodepa.erp.comptabilite.generale.application.usecase.SynchroniserReleveAutomatiqueUseCase;
import com.sodepa.erp.comptabilite.generale.presentation.requests.ReleveManuelRequest;
import com.sodepa.erp.comptabilite.generale.presentation.requests.SyncRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
