from flask import Flask, request, jsonify
from flask_cors import CORS
import base64
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import os
import pickle
from google.auth.transport.requests import Request
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build

app = Flask(__name__)
CORS(app)

SCOPES = ['https://www.googleapis.com/auth/gmail.send']
CREDENTIALS_PATH = 'confidential/credentials.json'
TOKEN_PATH = 'confidential/token.pickle'
DEFAULT_SENDER = 'sebastian.herrera45451@ucaldas.edu.co'


# ─── Gmail Auth ────────────────────────────────────────────────────────────────

def authenticate_gmail():
    creds = None
    if os.path.exists(TOKEN_PATH):
        with open(TOKEN_PATH, 'rb') as token:
            creds = pickle.load(token)
    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            creds.refresh(Request())
        else:
            flow = InstalledAppFlow.from_client_secrets_file(CREDENTIALS_PATH, SCOPES)
            creds = flow.run_local_server(port=0)
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


# ─── Endpoints ─────────────────────────────────────────────────────────────────

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok', 'service': 'ms-notificaciones'}), 200


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
        service = build('gmail', 'v1', credentials=creds)
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

    body = f"""
    <html><body>
    <p>Estimado/a <strong>{name}</strong>,</p>
    <p>Sus roles han sido actualizados en el sistema.</p>
    <p><strong>Acción:</strong> {action} del rol <em>"{role_name}"</em>.</p>
    <p>Este cambio aplica <strong>inmediatamente</strong> en la plataforma.</p>
    <br>
    <p>Si tiene alguna duda, contacte al administrador del sistema.</p>
    <p>Saludos,<br><strong>Equipo de Administración</strong></p>
    </body></html>
    """

    try:
        creds   = authenticate_gmail()
        service = build('gmail', 'v1', credentials=creds)
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

    body = f"""
    <html><body>
    <p>Estimado/a <strong>{name}</strong>,</p>
    <p>Los permisos del rol <em>"{role_name}"</em> han sido actualizados.</p>
    <p>Este cambio aplica <strong>inmediatamente</strong> en su acceso a la plataforma.</p>
    <br>
    <p>Si tiene alguna duda, contacte al administrador del sistema.</p>
    <p>Saludos,<br><strong>Equipo de Administración</strong></p>
    </body></html>
    """

    try:
        creds   = authenticate_gmail()
        service = build('gmail', 'v1', credentials=creds)
        message = build_message(DEFAULT_SENDER, to,
                                'Actualización de permisos - Flash Bus',
                                body, is_html=True)
        result  = send_gmail(service, message)
        return jsonify({'success': True, 'id': result['id']}), 200
    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
