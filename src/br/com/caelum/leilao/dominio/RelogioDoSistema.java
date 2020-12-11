package br.com.caelum.leilao.dominio;

import br.com.caelum.leilao.repository.Relogio;

import java.util.Calendar;

public class RelogioDoSistema implements Relogio {

    public Calendar hoje() {
        return Calendar.getInstance();
    }
}
