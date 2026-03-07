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

  onSocialLogin(provider: string): void {
    this.errorMessage = `${provider.charAt(0).toUpperCase() + provider.slice(1)}: disponible próximamente`;
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
