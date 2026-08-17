package com.sodepa.erp.comptabilite.generale.presentation.rest;

import com.sodepa.erp.comptabilite.generale.application.inputs.GetBalanceInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.GetGrandLivreInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.GetLivreJournalInput;
import com.sodepa.erp.comptabilite.generale.application.inputs.GetTvaDeclarationInput;
import com.sodepa.erp.comptabilite.generale.application.usecase.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/comptabilite/reporting")
@RequiredArgsConstructor
public class ReportingRestController {

    private final GetLivreJournalUseCase getLivreJournalUseCase;
    private final GetGrandLivreUseCase getGrandLivreUseCase;
    private final GetBalanceUseCase getBalanceUseCase;
    private final GetBilanUseCase getBilanUseCase;
    private final GetCompteResultatUseCase getCompteResultatUseCase;
    private final GetTftUseCase getTftUseCase;
    private final GetTvaDeclarationUseCase getTvaDeclarationUseCase;
    private final DownloadFecUseCase downloadFecUseCase;

    @GetMapping("/livre-journal")
    public List<ReportingUseCase.LivreJournalLine> getLivreJournal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return getLivreJournalUseCase.execute(new GetLivreJournalInput(debut, fin));
    }

    @GetMapping("/grand-livre")
    public List<ReportingUseCase.GrandLivreAccount> getGrandLivre(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return getGrandLivreUseCase.execute(new GetGrandLivreInput(debut, fin));
    }

    @GetMapping("/balance")
    public List<ReportingUseCase.BalanceLine> getBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return getBalanceUseCase.execute(new GetBalanceInput(debut, fin));
    }

    @GetMapping("/bilan")
    public ReportingUseCase.BilanReport getBilan(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateBilan) {
        return getBilanUseCase.execute(dateBilan);
    }

    @GetMapping("/compte-resultat")
    public ReportingUseCase.CompteResultatReport getCompteResultat(
            @RequestParam int annee) {
        return getCompteResultatUseCase.execute(annee);
    }

    @GetMapping("/tft")
    public ReportingUseCase.TftReport getTft(
            @RequestParam int annee) {
        return getTftUseCase.execute(annee);
    }

    @GetMapping("/tva")
    public ReportingUseCase.TvaDeclaration getTvaDeclaration(
            @RequestParam int annee,
            @RequestParam int mois) {
        return getTvaDeclarationUseCase.execute(new GetTvaDeclarationInput(annee, mois));
    }

    @GetMapping("/fec")
    public ResponseEntity<String> downloadFec(@RequestParam int annee) {
        String fecContent = downloadFecUseCase.execute(annee);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"FEC_" + annee + ".txt\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                .body(fecContent);
    }
}
