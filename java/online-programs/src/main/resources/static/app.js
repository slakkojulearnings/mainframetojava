const API_BASE = '/carddemo/api';

let sessionId = null;
let currentUser = null;

// Screen navigation
function showScreen(screenId) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    document.getElementById(screenId).classList.add('active');
}

// Signon
document.getElementById('signinForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const userId = document.getElementById('userId').value;
    const password = document.getElementById('password').value;
    const errorMsg = document.getElementById('errorMsg');

    try {
        const response = await fetch(`${API_BASE}/signon`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId, password }),
            credentials: 'include'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Sign in failed');
        }

        const data = await response.json();
        currentUser = data;
        loadMenu();
        showScreen('menuScreen');
        errorMsg.classList.remove('show');
    } catch (err) {
        errorMsg.textContent = err.message;
        errorMsg.classList.add('show');
    }
});

// Load menu
async function loadMenu() {
    try {
        const response = await fetch(`${API_BASE}/user/menu`, {
            credentials: 'include'
        });

        if (!response.ok) throw new Error('Failed to load menu');

        const data = await response.json();
        const menuOptions = document.getElementById('menuOptions');
        menuOptions.innerHTML = data.options.map(opt => `
            <div class="menu-item" onclick="handleMenuClick('${opt.program}', '${opt.name}')">
                <h3>${opt.number}</h3>
                <h3>${opt.name}</h3>
                <p>${opt.program}</p>
            </div>
        `).join('');
    } catch (err) {
        console.error('Menu load error:', err);
    }
}

// Handle menu click
async function handleMenuClick(program, name) {
    switch(program) {
        case 'COACTVWC':
            showAccountView();
            break;
        case 'COCRDLSC':
            showCardList();
            break;
        case 'COTRN02C':
            showTransactionList();
            break;
        default:
            alert(`Program ${program} not yet implemented`);
    }
}

// Account View
async function showAccountView() {
    const acctId = prompt('Enter Account ID:');
    if (!acctId) return;

    try {
        const response = await fetch(`${API_BASE}/account/${acctId}`, {
            credentials: 'include'
        });

        if (!response.ok) {
            const error = await response.json();
            alert(`Error: ${error.error}`);
            return;
        }

        const account = await response.json();
        const details = document.getElementById('accountDetails');

        details.innerHTML = `
            <div class="detail-item">
                <label>Account ID</label>
                <span>${account.accountId}</span>
            </div>
            <div class="detail-item">
                <label>Current Balance</label>
                <span>$${account.currBal}</span>
            </div>
            <div class="detail-item">
                <label>Credit Limit</label>
                <span>$${account.creditLimit}</span>
            </div>
            <div class="detail-item">
                <label>Customer Name</label>
                <span>${account.firstName} ${account.lastName}</span>
            </div>
            <div class="detail-item">
                <label>Status</label>
                <span>${account.activeStatus === 'A' ? 'Active' : 'Inactive'}</span>
            </div>
            <div class="detail-item">
                <label>SSN</label>
                <span>${account.ssn}</span>
            </div>
        `;

        showScreen('accountScreen');
        document.getElementById('backBtn').onclick = () => showScreen('menuScreen');
    } catch (err) {
        alert('Error loading account: ' + err.message);
    }
}

// Card List
async function showCardList() {
    try {
        const response = await fetch(`${API_BASE}/cards`, {
            credentials: 'include'
        });

        if (!response.ok) throw new Error('Failed to load cards');

        const { cards } = await response.json();
        const cardList = document.getElementById('cardList');

        cardList.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>Card Number</th>
                        <th>Name</th>
                        <th>Status</th>
                        <th>Expiration</th>
                    </tr>
                </thead>
                <tbody>
                    ${cards.map(c => `
                        <tr>
                            <td>${c.cardNum.slice(0, 4)} **** **** ${c.cardNum.slice(-4)}</td>
                            <td>${c.embossedName}</td>
                            <td>${c.activeStatus === 'A' ? 'Active' : 'Inactive'}</td>
                            <td>${c.expirationDate}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;

        showScreen('cardListScreen');
        document.getElementById('backBtn2').onclick = () => showScreen('menuScreen');
    } catch (err) {
        alert('Error loading cards: ' + err.message);
    }
}

// Transaction List
async function showTransactionList() {
    try {
        const response = await fetch(`${API_BASE}/transactions`, {
            credentials: 'include'
        });

        if (!response.ok) throw new Error('Failed to load transactions');

        const { transactions } = await response.json();
        const tranList = document.getElementById('tranList');

        tranList.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Amount</th>
                        <th>Type</th>
                        <th>Description</th>
                    </tr>
                </thead>
                <tbody>
                    ${transactions.map(t => `
                        <tr>
                            <td>${t.procTs}</td>
                            <td>$${t.amount}</td>
                            <td>${t.typeCode}</td>
                            <td>${t.desc}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;

        showScreen('tranListScreen');
        document.getElementById('backBtn3').onclick = () => showScreen('menuScreen');
    } catch (err) {
        alert('Error loading transactions: ' + err.message);
    }
}

// Signout
document.getElementById('signoutBtn').addEventListener('click', () => {
    sessionId = null;
    currentUser = null;
    document.getElementById('signinForm').reset();
    document.getElementById('errorMsg').classList.remove('show');
    showScreen('loginScreen');
});

// Initialize
showScreen('loginScreen');
