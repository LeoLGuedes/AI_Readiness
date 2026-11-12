package br.com.prontidaoia.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Campos aceitos do navegador; o papel nunca faz parte do formulario. */
public class CadastroForm {
    @NotBlank(message = "Informe seu nome.")
    @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe seu e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 160, message = "E-mail deve ter no máximo 160 caracteres.")
    private String email;

    @NotNull(message = "Selecione um departamento.")
    private Long departamentoId;

    @NotBlank(message = "Crie uma senha.")
    private String senha;
    @NotBlank(message = "Confirme a senha.")
    private String confirmacao;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getDepartamentoId() { return departamentoId; }
    public void setDepartamentoId(Long departamentoId) { this.departamentoId = departamentoId; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getConfirmacao() { return confirmacao; }
    public void setConfirmacao(String confirmacao) { this.confirmacao = confirmacao; }
}
