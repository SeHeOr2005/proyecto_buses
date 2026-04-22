from flask import Flask, request, jsonify
import base64
from html import escape
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import os
import pickle
import json
from textwrap import dedent

app = Flask(__name__)

SCOPES = ['https://www.googleapis.com/auth/gmail.send']
CREDENTIALS_PATH = os.getenv('GOOGLE_CREDENTIALS_PATH', 'confidential/credentials.json')
TOKEN_PATH = os.getenv('GOOGLE_TOKEN_PATH', 'confidential/token.pickle')
DEFAULT_SENDER = os.getenv('GMAIL_SENDER', 'sebastian.herrera45451@ucaldas.edu.co')
GOOGLE_CREDENTIALS_JSON = os.getenv('GOOGLE_CREDENTIALS_JSON', '')
GOOGLE_CREDENTIALS_JSON_BASE64 = os.getenv('GOOGLE_CREDENTIALS_JSON_BASE64', '')
GOOGLE_TOKEN_PICKLE_BASE64 = os.getenv('GOOGLE_TOKEN_PICKLE_BASE64', '')
ALLOW_INTERACTIVE_OAUTH = os.getenv('ALLOW_INTERACTIVE_OAUTH', 'false').lower() in ('1', 'true', 'yes', 'on')
LOGO_PATHS = ['flash-bus-logo-cropped.png', 'flash-bus-logo.png']


# ─── Gmail Auth ────────────────────────────────────────────────────────────────

def load_client_config_from_env():
    if GOOGLE_CREDENTIALS_JSON:
        return json.loads(GOOGLE_CREDENTIALS_JSON)
    if GOOGLE_CREDENTIALS_JSON_BASE64:
        decoded = base64.b64decode(GOOGLE_CREDENTIALS_JSON_BASE64).decode('utf-8')
        return json.loads(decoded)
    return None


def load_token_from_env():
    if not GOOGLE_TOKEN_PICKLE_BASE64:
        return None
    token_bytes = base64.b64decode(GOOGLE_TOKEN_PICKLE_BASE64)
    return pickle.loads(token_bytes)


def build_gmail_service(credentials):
    from googleapiclient.discovery import build

    return build('gmail', 'v1', credentials=credentials)


def refresh_google_credentials(credentials):
    from google.auth.transport.requests import Request

    credentials.refresh(Request())
    return credentials

def authenticate_gmail():
    creds = load_token_from_env()

    if not creds and os.path.exists(TOKEN_PATH):
        with open(TOKEN_PATH, 'rb') as token:
            creds = pickle.load(token)

    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            creds = refresh_google_credentials(creds)
        else:
            if not ALLOW_INTERACTIVE_OAUTH:
                raise RuntimeError(
                    'No hay token OAuth válido y ALLOW_INTERACTIVE_OAUTH está en false. '
                    'Define GOOGLE_TOKEN_PICKLE_BASE64 (recomendado) o habilita OAuth interactivo temporalmente.'
                )

            from google_auth_oauthlib.flow import InstalledAppFlow

            client_config = load_client_config_from_env()
            if client_config:
                flow = InstalledAppFlow.from_client_config(client_config, SCOPES)
            elif os.path.exists(CREDENTIALS_PATH):
                flow = InstalledAppFlow.from_client_secrets_file(CREDENTIALS_PATH, SCOPES)
            else:
                raise RuntimeError(
                    'No se encontraron credenciales OAuth de Google. '
                    'Define GOOGLE_CREDENTIALS_JSON o GOOGLE_CREDENTIALS_JSON_BASE64.'
                )

            creds = flow.run_local_server(port=0)

        token_dir = os.path.dirname(TOKEN_PATH)
        if token_dir:
            os.makedirs(token_dir, exist_ok=True)
        with open(TOKEN_PATH, 'wb') as token:
            pickle.dump(creds, token)
    return creds


def build_message(sender, to, subject, body, is_html=False):
    if is_html:
        msg = MIMEMultipart('alternative')
        msg['to'] = to
        msg['from'] = sender
        msg['subject'] = subject
        msg.attach(MIMEText(body, 'html'))
    else:
        msg = MIMEText(body)
        msg['to'] = to
        msg['from'] = sender
        msg['subject'] = subject
    raw = base64.urlsafe_b64encode(msg.as_bytes()).decode()
    return {'raw': raw}


def send_gmail(service, message):
    return service.users().messages().send(userId='me', body=message).execute()


def get_logo_data_uri():
    for logo_path in LOGO_PATHS:
        if os.path.exists(logo_path):
            with open(logo_path, 'rb') as logo_file:
                encoded_logo = base64.b64encode(logo_file.read()).decode('ascii')
            return f'data:image/png;base64,{encoded_logo}'
    return ''


def build_email_template(
        title,
        headline,
        intro,
        body_html,
        footer_note,
        accent_color='#0d59cf',
        badge_text='Flash Bus',
        action_text=None,
        action_url=None,
    logo_src='',
):
        safe_action_text = escape(action_text) if action_text else ''
        safe_action_url = escape(action_url, quote=True) if action_url else ''
        action_block = ''
        if safe_action_text and safe_action_url:
                action_block = f'<a class="button" href="{safe_action_url}">{safe_action_text}</a>'

        return dedent(f'''
        <!DOCTYPE html>
        <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>{escape(title)}</title>
                <style>
                    body {{
                        margin: 0;
                        padding: 0;
                        background: #f4f7fb;
                        font-family: Arial, Helvetica, sans-serif;
                        color: #1f2937;
                    }}
                    .wrapper {{
                        width: 100%;
                        background: linear-gradient(180deg, #eaf1ff 0%, #f4f7fb 38%, #f4f7fb 100%);
                        padding: 32px 16px;
                    }}
                    .container {{
                        max-width: 640px;
                        margin: 0 auto;
                    }}
                    .card {{
                        background: #ffffff;
                        border-radius: 22px;
                        overflow: hidden;
                        box-shadow: 0 18px 48px rgba(15, 23, 42, 0.12);
                        border: 1px solid rgba(13, 89, 207, 0.08);
                    }}
                    .header {{
                        padding: 28px 32px 18px;
                        background: linear-gradient(135deg, {accent_color} 0%, #0b3f99 100%);
                        color: #ffffff;
                    }}
                    .badge {{
                        display: inline-block;
                        padding: 6px 12px;
                        border-radius: 999px;
                        background: rgba(255, 255, 255, 0.18);
                        font-size: 12px;
                        font-weight: 700;
                        letter-spacing: 0.08em;
                        text-transform: uppercase;
                        margin-bottom: 16px;
                    }}
                    .brand {{
                        display: flex;
                        align-items: center;
                        gap: 14px;
                    }}
                    .brand-mark {{
                        width: 48px;
                        height: 48px;
                        border-radius: 14px;
                        background: rgba(255, 255, 255, 0.18);
                        display: inline-flex;
                        align-items: center;
                        justify-content: center;
                        font-size: 18px;
                        font-weight: 800;
                        letter-spacing: 0.04em;
                    }}
                    .brand-logo {{
                        display: block;
                        max-width: 220px;
                        max-height: 72px;
                        width: auto;
                        height: auto;
                        object-fit: contain;
                    }}
                    .brand-text h1 {{
                        margin: 0;
                        font-size: 20px;
                        line-height: 1.2;
                    }}
                    .brand-text p {{
                        margin: 4px 0 0;
                        font-size: 14px;
                        opacity: 0.9;
                    }}
                    .content {{
                        padding: 32px;
                    }}
                    .eyebrow {{
                        margin: 0 0 8px;
                        color: {accent_color};
                        font-size: 13px;
                        font-weight: 700;
                        letter-spacing: 0.06em;
                        text-transform: uppercase;
                    }}
                    .headline {{
                        margin: 0 0 14px;
                        font-size: 28px;
                        line-height: 1.2;
                        color: #0f172a;
                    }}
                    .intro, .paragraph {{
                        margin: 0 0 16px;
                        font-size: 16px;
                        line-height: 1.7;
                        color: #334155;
                    }}
                    .panel {{
                        margin: 24px 0;
                        padding: 20px;
                        border-radius: 18px;
                        background: #f8fbff;
                        border: 1px solid rgba(13, 89, 207, 0.12);
                    }}
                    .button {{
                        display: inline-block;
                        margin-top: 8px;
                        padding: 14px 22px;
                        border-radius: 14px;
                        background: {accent_color};
                        color: #ffffff !important;
                        text-decoration: none;
                        font-weight: 700;
                        font-size: 15px;
                    }}
                    .code-box {{
                        display: inline-block;
                        padding: 16px 22px;
                        border-radius: 16px;
                        background: linear-gradient(135deg, rgba(13, 89, 207, 0.10), rgba(13, 89, 207, 0.18));
                        border: 1px solid rgba(13, 89, 207, 0.18);
                        font-size: 32px;
                        line-height: 1;
                        letter-spacing: 0.18em;
                        font-weight: 800;
                        color: #0b3f99;
                    }}
                    .meta {{
                        margin-top: 12px;
                        font-size: 14px;
                        color: #64748b;
                    }}
                    .fine-print {{
                        margin: 18px 0 0;
                        font-size: 13px;
                        line-height: 1.6;
                        color: #64748b;
                    }}
                    .footer {{
                        padding: 0 32px 28px;
                        color: #64748b;
                        font-size: 13px;
                        line-height: 1.6;
                    }}
                    .divider {{
                        margin: 0 32px 20px;
                        height: 1px;
                        background: linear-gradient(90deg, rgba(13, 89, 207, 0), rgba(13, 89, 207, 0.18), rgba(13, 89, 207, 0));
                    }}
                    @media only screen and (max-width: 600px) {{
                        .wrapper {{ padding: 16px 10px; }}
                        .header, .content, .footer, .divider {{ padding-left: 20px; padding-right: 20px; }}
                        .headline {{ font-size: 24px; }}
                        .code-box {{ font-size: 26px; letter-spacing: 0.12em; }}
                        .brand-text h1 {{ font-size: 18px; }}
                    }}
                </style>
            </head>
            <body>
                <div class="wrapper">
                    <div class="container">
                        <div class="card">
                            <div class="header">
                                <div class="brand">
                                    {f'<img class="brand-logo" src="{escape(logo_src, quote=True)}" alt="Flash Bus">' if logo_src else '<div class="brand-mark">FB</div>'}
                                </div>
                            </div>
                            <div class="content">
                                <p class="eyebrow">{escape(badge_text)}</p>
                                <h2 class="headline">{escape(headline)}</h2>
                                <p class="intro">{intro}</p>
                                {body_html}
                                {action_block}
                            </div>
                            <div class="divider"></div>
                            <div class="footer">
                                {footer_note}
                            </div>
                        </div>
                    </div>
                </div>
            </body>
        </html>
        ''').strip()


def render_role_change_email(name, role_name, action):
        safe_name = escape(name)
        safe_role_name = escape(role_name)
        safe_action = escape(action)
        body_html = f'''
        <div class="panel">
            <p class="paragraph"><strong>Usuario:</strong> {safe_name}</p>
            <p class="paragraph"><strong>Acción:</strong> {safe_action} del rol <em>{safe_role_name}</em></p>
            <p class="paragraph" style="margin-bottom: 0;">Este cambio aplica inmediatamente en la plataforma.</p>
        </div>
        <p class="paragraph">Si tienes alguna duda, contacta al administrador del sistema.</p>
        '''
        return build_email_template(
                title='Flash Bus',
                headline='Tus roles fueron actualizados',
                intro=f'Estimado/a <strong>{safe_name}</strong>, hemos actualizado tus permisos dentro de la plataforma.',
                body_html=body_html,
                footer_note='Saludos,<br><strong>Equipo de Administración</strong>',
                accent_color='#0d59cf',
                logo_src=get_logo_data_uri(),
        )


def render_permission_change_email(name, role_name):
        safe_name = escape(name)
        safe_role_name = escape(role_name)
        body_html = f'''
        <div class="panel">
            <p class="paragraph" style="margin-bottom: 0;">Los permisos del rol <strong>{safe_role_name}</strong> se han ajustado para reflejar el nuevo acceso asignado.</p>
        </div>
        <p class="paragraph">Estos cambios aplican inmediatamente en tu cuenta.</p>
        <p class="paragraph">Si tienes alguna duda, contacta al administrador del sistema.</p>
        '''
        return build_email_template(
                title='Flash Bus',
                headline='Actualización de permisos',
                intro=f'Hola <strong>{safe_name}</strong>, te informamos que tu acceso fue actualizado.',
                body_html=body_html,
                footer_note='Saludos,<br><strong>Equipo de Administración</strong>',
                accent_color='#0d59cf',
                logo_src=get_logo_data_uri(),
        )


def render_account_confirmation_email(name):
        safe_name = escape(name)
        body_html = '''
        <div class="panel">
            <p class="paragraph" style="margin-bottom: 0;">Ya puedes ingresar a la plataforma con tu correo y contraseña.</p>
        </div>
        <p class="paragraph">Si no reconoces este registro, contacta al equipo de soporte.</p>
        '''
        return build_email_template(
                title='Flash Bus',
                headline='Tu cuenta fue creada con éxito',
                intro=f'Hola <strong>{safe_name}</strong>, tu cuenta en <strong>Flash Bus</strong> está lista para usar.',
                body_html=body_html,
                footer_note='Saludos,<br><strong>Equipo Flash Bus</strong>',
                accent_color='#0f766e',
                logo_src=get_logo_data_uri(),
        )


def render_password_recovery_email(name, recovery_link, expires_in_minutes):
        safe_name = escape(name)
        body_html = f'''
        <div class="panel">
            <p class="paragraph" style="margin-bottom: 14px;">Haz clic en el botón para continuar con el restablecimiento de contraseña.</p>
            <p class="paragraph" style="margin-bottom: 0;">Este enlace solo debe usarse desde un dispositivo de confianza.</p>
        </div>
        <p class="meta">Este enlace estará disponible por <strong>{expires_in_minutes} minutos</strong>.</p>
        '''
        return build_email_template(
                title='Flash Bus',
                headline='Restablece tu contraseña',
                intro=f'Hola <strong>{safe_name}</strong>, recibimos una solicitud para recuperar el acceso a tu cuenta.',
                body_html=body_html,
                footer_note='Si no solicitaste este cambio, puedes ignorar este mensaje.<br><br>Saludos,<br><strong>Equipo Flash Bus</strong>',
                accent_color='#c2410c',
                action_text='Restablecer contraseña',
                action_url=recovery_link,
                logo_src=get_logo_data_uri(),
        )


def render_two_factor_email(name, code, expires_in_minutes):
        safe_name = escape(name)
        safe_code = escape(code)
        body_html = f'''
        <div class="panel" style="text-align: center;">
            <div class="code-box">{safe_code}</div>
            <p class="meta">Código válido por <strong>{expires_in_minutes} minutos</strong></p>
        </div>
        <p class="paragraph">Si no solicitaste este acceso, ignora este mensaje.</p>
        '''
        return build_email_template(
                title='Flash Bus',
                headline='Código de verificación para iniciar sesión',
                intro=f'Hola <strong>{safe_name}</strong>, usa el siguiente código para completar tu inicio de sesión.',
                body_html=body_html,
                footer_note='Mantén este código en privado.<br><br>Saludos,<br><strong>Equipo Flash Bus</strong>',
                accent_color='#1d4ed8',
                logo_src=get_logo_data_uri(),
        )


# ─── Endpoints ─────────────────────────────────────────────────────────────────

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok', 'service': 'ms-notificaciones'}), 200


@app.route('/', methods=['GET'])
def index():
    return jsonify({'status': 'ok', 'service': 'ms-notificaciones', 'message': 'running'}), 200


@app.route('/send-email', methods=['POST'])
def send_email():
    """
    Envía un correo usando Gmail API.
    Body JSON esperado:
    {
        "to":      "destino@correo.com",   (requerido)
        "subject": "Asunto",               (opcional, default: 'Notificación')
        "body":    "Contenido del correo", (requerido)
        "is_html": false                   (opcional, default: false)
    }
    """
    data = request.get_json()

    if not data:
        return jsonify({'error': 'No se recibieron datos'}), 400

    to      = data.get('to')
    body    = data.get('body')
    subject = data.get('subject', 'Notificación del Sistema')
    is_html = data.get('is_html', False)

    if not to:
        return jsonify({'error': 'El campo "to" es requerido'}), 400
    if not body:
        return jsonify({'error': 'El campo "body" es requerido'}), 400

    try:
        creds   = authenticate_gmail()
        service = build_gmail_service(creds)
        message = build_message(DEFAULT_SENDER, to, subject, body, is_html)
        result  = send_gmail(service, message)
        return jsonify({
            'success': True,
            'message': 'Correo enviado exitosamente',
            'id': result['id']
        }), 200
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500


@app.route('/send-role-change', methods=['POST'])
def send_role_change():
    """
    Notificación específica para cambio de rol (HU-ENTR-1-002).
    Body JSON:
    {
        "to":       "usuario@correo.com",
        "name":     "Nombre del usuario",
        "roleName": "ADMINISTRADOR_SISTEMA",
        "action":   "Asignación" | "Revocación"
    }
    """
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No se recibieron datos'}), 400

    to        = data.get('to')
    name      = data.get('name', 'Usuario')
    role_name = data.get('roleName', '')
    action    = data.get('action', 'Actualización')

    if not to:
        return jsonify({'error': 'El campo "to" es requerido'}), 400

    body = render_role_change_email(name, role_name, action)

    try:
        creds   = authenticate_gmail()
        service = build_gmail_service(creds)
        message = build_message(DEFAULT_SENDER, to,
                                'Cambio en sus roles y permisos - Flash Bus',
                                body, is_html=True)
        result  = send_gmail(service, message)
        return jsonify({'success': True, 'id': result['id']}), 200
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500


@app.route('/send-permission-change', methods=['POST'])
def send_permission_change():
    """
    Notificación específica para cambio de permisos (HU-ENTR-1-003).
    Body JSON:
    {
        "to":       "usuario@correo.com",
        "name":     "Nombre del usuario",
        "roleName": "SUPERVISOR"
    }
    """
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No se recibieron datos'}), 400

    to        = data.get('to')
    name      = data.get('name', 'Usuario')
    role_name = data.get('roleName', '')

    if not to:
        return jsonify({'error': 'El campo "to" es requerido'}), 400

    body = render_permission_change_email(name, role_name)

    try:
        creds   = authenticate_gmail()
        service = build_gmail_service(creds)
        message = build_message(DEFAULT_SENDER, to,
                                'Actualización de permisos - Flash Bus',
                                body, is_html=True)
        result  = send_gmail(service, message)
        return jsonify({'success': True, 'id': result['id']}), 200
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500


@app.route('/send-account-confirmation', methods=['POST'])
def send_account_confirmation():
    """
    Notificación de confirmación de cuenta creada.
    Body JSON:
    {
        "to":   "usuario@correo.com",
        "name": "Nombre del usuario"
    }
    """
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No se recibieron datos'}), 400

    to = data.get('to')
    name = data.get('name', 'Usuario')

    if not to:
        return jsonify({'error': 'El campo "to" es requerido'}), 400

    body = render_account_confirmation_email(name)

    try:
        creds = authenticate_gmail()
        service = build_gmail_service(creds)
        message = build_message(DEFAULT_SENDER, to,
                                'Confirmación de cuenta - Flash Bus',
                                body, is_html=True)
        result = send_gmail(service, message)
        return jsonify({'success': True, 'id': result['id']}), 200
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500


@app.route('/send-password-recovery', methods=['POST'])
def send_password_recovery():
    """
    Notificación de recuperación de contraseña.
    Body JSON:
    {
        "to": "usuario@correo.com",
        "name": "Nombre del usuario",
        "recoveryLink": "https://...",
        "expiresInMinutes": 30
    }
    """
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No se recibieron datos'}), 400

    to = data.get('to')
    name = data.get('name', 'Usuario')
    recovery_link = data.get('recoveryLink', '')
    expires_in_minutes = data.get('expiresInMinutes', 30)

    if not to:
        return jsonify({'error': 'El campo "to" es requerido'}), 400
    if not recovery_link:
        return jsonify({'error': 'El campo "recoveryLink" es requerido'}), 400

    body = render_password_recovery_email(name, recovery_link, expires_in_minutes)

    try:
        creds = authenticate_gmail()
        service = build_gmail_service(creds)
        message = build_message(DEFAULT_SENDER, to,
                                'Recuperación de contraseña - Flash Bus',
                                body, is_html=True)
        result = send_gmail(service, message)
        return jsonify({'success': True, 'id': result['id']}), 200
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500


@app.route('/send-2fa-code', methods=['POST'])
def send_two_factor_code():
    """
    Notificación de código 2FA por correo.
    Body JSON:
    {
        "to": "usuario@correo.com",
        "name": "Nombre del usuario",
        "code": "123456",
        "expiresInMinutes": 3
    }
    """
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No se recibieron datos'}), 400

    to = data.get('to')
    name = data.get('name', 'Usuario')
    code = data.get('code')
    expires_in_minutes = data.get('expiresInMinutes', 3)

    if not to:
        return jsonify({'error': 'El campo "to" es requerido'}), 400
    if not code:
        return jsonify({'error': 'El campo "code" es requerido'}), 400

    body = render_two_factor_email(name, code, expires_in_minutes)

    try:
        creds = authenticate_gmail()
        service = build_gmail_service(creds)
        message = build_message(DEFAULT_SENDER, to,
                                'Código de verificación 2FA - Flash Bus',
                                body, is_html=True)
        result = send_gmail(service, message)
        return jsonify({'success': True, 'id': result['id']}), 200
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500


if __name__ == '__main__':
    port = int(os.getenv('PORT', '5000'))
    debug = os.getenv('FLASK_DEBUG', 'false').lower() in ('1', 'true', 'yes', 'on')
    app.run(debug=debug, host='0.0.0.0', port=port)
