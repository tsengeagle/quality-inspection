function toBase64Url(value) {
  return Buffer.from(value)
    .toString('base64')
    .replace(/=+$/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

function buildDemoToken() {
  const userId = process.env.QI_DEMO_USER_ID || 'demo-manager';
  const userName = process.env.QI_DEMO_USER_NAME || 'Demo Manager';
  const roleName = process.env.QI_DEMO_ROLE || 'hospital_manager';
  const hospitalId = process.env.QI_DEMO_HOSPITAL_ID || 'H001';

  const payload = {
    'tz-user': JSON.stringify({
      user: {
        user_id: userId,
        user_name: userName,
        user_role: [{ role_name: roleName, enabled: true }],
        hospital_id: hospitalId
      }
    })
  };

  return `eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.${toBase64Url(JSON.stringify(payload))}.demo`;
}

const target = process.env.QI_PROXY_TARGET || 'http://localhost:8080';
const apiKey = process.env.QI_API_KEY || buildDemoToken();

module.exports = {
  '/api/quality-inspection': {
    target,
    secure: false,
    changeOrigin: true,
    headers: {
      'api-key': apiKey
    },
    logLevel: process.env.QI_PROXY_LOG_LEVEL || 'debug'
  }
};
