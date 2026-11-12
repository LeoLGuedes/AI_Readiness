package br.com.prontidaoia.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edicao da propria conta; nenhum id de usuario ou papel vem do cliente. */
public class PerfilForm {
    @NotBlank(message = "Informe seu nome.")
    @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe seu e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 160, message = "E-mail deve ter no máximo 160 caracteres.")
    private String email;

    @NotNull(message = "Selecione um departamento.")
    private Long departamentoId;

    @NotBlank(message = "Confirme sua senha atual para salvar.")
    private String senhaAtual;
    private String novaSenha;
    private String confirmacaoNovaSenha;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getDepartamentoId() { return departamentoId; }
    public void setDepartamentoId(Long departamentoId) { this.departamentoId = departamentoId; }
    public String getSenhaAtual() { return senhaAtual; }
    public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }
    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
    public String getConfirmacaoNovaSenha() { return confirmacaoNovaSenha; }
    public void setConfirmacaoNovaSenha(String confirmacaoNovaSenha) { this.confirmacaoNovaSenha = confirmacaoNovaSenha; }
}
