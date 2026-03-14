import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  email = '';
  password = '';
  errorMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  loginGoogle(): void {
    window.location.href = 'http://localhost:8081/oauth2/authorization/google';
  }

  loginGitHub(): void {
    const clientId = 'Ov23ligZM0UoBxmlXOSG';
    const redirectUri = encodeURIComponent('http://localhost:8081/auth/github/callback');
    window.location.href = `https://github.com/login/oauth/authorize?client_id=${clientId}&redirect_uri=${redirectUri}`;
  }

  onSocialLogin(provider: string): void {
    if (provider === 'google') {
      this.loginGoogle();
    } else if (provider === 'github') {
      this.loginGitHub();
    } else {
      this.errorMessage = `${provider.charAt(0).toUpperCase() + provider.slice(1)}: disponible próximamente`;
    }
  }

  onSubmit(): void {
    this.errorMessage = '';

    this.authService.login(this.email, this.password).subscribe({
      next: (response) => {
        this.authService.saveToken(response.token);
        this.authService.saveUserEmail(this.email);
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.errorMessage = err.error || 'Email o contraseña incorrectos';
      }
    });
  }
}
