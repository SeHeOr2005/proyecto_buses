import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

/**
 * Recibe la redirección del backend tras login con GitHub (o similar).
 * Lee token, name, email, picture de la query y los guarda; luego redirige a /home.
 */
@Component({
  selector: 'app-auth-callback',
  standalone: true,
  template: `<p class="callback-message">Iniciando sesión...</p>`,
  styles: [`.callback-message { text-align: center; padding: 2rem; font-size: 1.1rem; }`]
})
export class AuthCallbackComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      const error = params['error'];

      if (error) {
        this.router.navigate(['/login'], { queryParams: { error } });
        return;
      }

      if (token) {
        this.authService.saveToken(token);
        this.authService.saveUserEmail(params['email'] ?? '');
        this.authService.saveUserName(params['name'] ?? '');
        this.authService.saveUserPicture(params['picture'] ?? null);
        this.router.navigate(['/home'], { replaceUrl: true });
      } else {
        this.router.navigate(['/login']);
      }
    });
  }
}
