package br.com.caelum.leilao.dominio;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.repository.Carteiro;
import br.com.caelum.leilao.repository.EnviadorDeEmail;
import br.com.caelum.leilao.repository.RepositorioDeLeiloes;
import br.com.caelum.leilao.servico.EncerradorDeLeilao;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EncerradorDeLeilaoTest {


    @Test
    public void deveEncerrarLeiloesQueComecaramUmaSemanaAtras() {

        Calendar antiga = Calendar.getInstance();
        antiga.set(1999, 1, 20);

        Leilao leilao1 = new CriadorDeLeilao().para("TV").naData(antiga).constroi();
        Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();


        Carteiro carteiro = mock(Carteiro.class);
        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));


        EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiro);
        encerrador.encerra();

        assertEquals(2, encerrador.getTotalEncerrados());
        assertTrue(leilao1.isEncerrado());
        assertTrue(leilao2.isEncerrado());

        verify(daoFalso, times(1)).atualiza(leilao1);
        verify(daoFalso, times(1)).atualiza(leilao2);

    }

    @Test
    public void naoDeveEncerrarLeiloesQueComecaramMenosDeUmaSemanaAtras() {
        Calendar ontem = Calendar.getInstance();
        ontem.add(Calendar.DAY_OF_MONTH, -1);

        Leilao leilao1 = new CriadorDeLeilao().para("TV").naData(ontem).constroi();

        Carteiro carteiro = mock(Carteiro.class);
        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1));

        EncerradorDeLeilao encerradorDeLeilao = new EncerradorDeLeilao(daoFalso, carteiro);
        encerradorDeLeilao.encerra();

        assertEquals(0, encerradorDeLeilao.getTotalEncerrados());
        assertFalse(leilao1.isEncerrado());
        verify(daoFalso, never()).atualiza(leilao1);
    }

    @Test
    public void naoDeveEncerrarLeiloesComListaVazia() {

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(new ArrayList<Leilao>());

        Carteiro carteiro = mock(Carteiro.class);
        EncerradorDeLeilao encerradorDeLeilao = new EncerradorDeLeilao(daoFalso, carteiro);
        encerradorDeLeilao.encerra();

        assertEquals(0, encerradorDeLeilao.getTotalEncerrados());

    }

    @Test
    public void deveEnviarEmailAposPersistirLeilaoEncerrado() {
        Calendar antiga = Calendar.getInstance();
        antiga.set(1999, 1, 20);

        Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma")
                .naData(antiga).constroi();

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1));

        Carteiro carteiro = mock(Carteiro.class);
        EncerradorDeLeilao encerrador =
                new EncerradorDeLeilao(daoFalso, carteiro);

        encerrador.encerra();

        InOrder inOrder = inOrder(daoFalso, carteiro);
        inOrder.verify(daoFalso, times(1)).atualiza(leilao1);
        inOrder.verify(carteiro, times(1)).envia(leilao1);
    }

    @Test
    public void deveContinuarAExecucaoMesmoQuandoDaoFalha() {
        Calendar antiga = Calendar.getInstance();
        antiga.set(1999, 1, 20);

        Leilao leilao1 = new CriadorDeLeilao()
                .para("TV")
                .naData(antiga)
                .constroi();

        Leilao leilao2 = new CriadorDeLeilao()
                .para("Geladeira")
                .naData(antiga)
                .constroi();

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

        doThrow(new RuntimeException()).when(daoFalso).atualiza(leilao1);

        Carteiro carteiroFalso = mock(Carteiro.class);
        EncerradorDeLeilao encerradorDeLeilao = new EncerradorDeLeilao(daoFalso, carteiroFalso);

        encerradorDeLeilao.encerra();

        verify(daoFalso).atualiza(leilao2);
        verify(carteiroFalso).envia(leilao2);
    }

    @Test
    public void deveContinuarAExecucaoMesmoQuandoEnviadorDeEmaillFalha() {
        Calendar antiga = Calendar.getInstance();
        antiga.set(1999, 1, 20);

        Leilao leilao1 = new CriadorDeLeilao()
                .para("TV")
                .naData(antiga)
                .constroi();

        Leilao leilao2 = new CriadorDeLeilao()
                .para("Geladeira")
                .naData(antiga)
                .constroi();

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

        Carteiro carteiroFalso = mock(Carteiro.class);
        doThrow(new RuntimeException()).when(carteiroFalso).envia(leilao1);
        EncerradorDeLeilao encerradorDeLeilao = new EncerradorDeLeilao(daoFalso, carteiroFalso);

        encerradorDeLeilao.encerra();

        verify(daoFalso).atualiza(leilao2);
        verify(carteiroFalso).envia(leilao2);
    }

    @Test
    public void naoInvocarCarteiro() {
        Calendar antiga = Calendar.getInstance();
        antiga.set(1999, 1, 20);

        Leilao leilao1 = new CriadorDeLeilao()
                .para("TV")
                .naData(antiga)
                .constroi();

        Leilao leilao2 = new CriadorDeLeilao()
                .para("Geladeira")
                .naData(antiga)
                .constroi();

        RepositorioDeLeiloes daoFalso = mock(RepositorioDeLeiloes.class);
        when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
//        doThrow(new RuntimeException()).when(daoFalso).atualiza(leilao1);
//        doThrow(new RuntimeException()).when(daoFalso).atualiza(leilao2);
        doThrow(new RuntimeException()).when(daoFalso).atualiza(any(Leilao.class));

        Carteiro carteiroFalso = mock(Carteiro.class);

        EncerradorDeLeilao encerradorDeLeilao = new EncerradorDeLeilao(daoFalso, carteiroFalso);
        encerradorDeLeilao.encerra();

//        verify(carteiroFalso, never()).envia(leilao1);
//        verify(carteiroFalso, never()).envia(leilao2);
        verify(carteiroFalso, never()).envia(any(Leilao.class));
    }



}
