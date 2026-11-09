# HishabKhata & BillBook: Retail Billing, Digital Ledger & Bilingual Voice Assistant

A modern, offline-first Android billing and business management app combining the speed of **MyBillBook** with the digital ledger simplicity of **TallyKhata** (হিসাবখাতা). It empowers grocery, retail, and general store merchants with multi-format invoice printing, inventory stock calculations, customer/supplier khata (Dena/Paona), bKash/WhatsApp payment reminders, and a fast bilingual (English & Bengali) voice assistant with custom shortcuts.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were synthesized from your prompt requirements and Phase 1 questionnaire responses:

- **Confirmed Decision 1 (Retail Grocery Priority)**: The catalog and billing flows are optimized for quick-item retail entry (touch quick-buttons, barcode/SKU quick lookup, and voice shortcut aliases like `"star 100"` for 100 units of Star/tobacco).
- **Confirmed Decision 2 (English UI + Bengali & English Voice)**: Clean, high-contrast English Material 3 interface with complete Bengali (বাংলা) voice command recognition and audio feedback.
- **Confirmed Decision 3 (All 3 Invoice Formats Supported)**: Built-in layout engine supporting:
  1. **58mm / 80mm Thermal POS Receipt** (optimized for ESC/POS Bluetooth printers and standard mobile receipt printers).
  2. **Full-Page A4 Detailed Tax/Bill Invoice** (via Android PrintManager / PDF generator).
  3. **Compact Digital WhatsApp Memo Card** (formatted plain-text & visual share card for WhatsApp and SMS).
- **Confirmed Decision 4 (Offline-First Privacy + Cloud Sync)**: Local Room database with encrypted storage for instant offline resilience, integrated with Firebase Firestore and Google Sign-In for cloud backup and multi-device durability.

---

## 1. Overview & Core Concept

**HishabKhata & BillBook** solves the daily operational hurdles faced by retail shop owners and small merchants. Instead of juggling paper khatas, bulky desktop software, and complex POS machines, merchants can manage their entire counter workflow from their Android phone:

- **Ultra-Fast Billing**: Scan, tap, or speak items into an active bill in seconds.
- **Multi-Format Printing & Sharing**: Direct Android PrintManager thermal receipt generation, PDF invoice creation, and instant WhatsApp / SMS payment memos with bKash payment links.
- **Stock & Inventory Engine**: Track purchase price, selling price, real-time stock levels, low-stock warnings, and profit margin analysis.
- **TallyKhata Ledger (Dena / Paona - দেনা / পাওনা)**: Dedicated Customer (Customer Khata) and Supplier (Supplier Khata) accounts tracking credit sales, lending, borrowing, payment histories, and automated reminder alerts.
- **Bilingual AI Voice Assistant**: Listens in English or Bengali (even fast speech) to execute commands such as `"star 100"` (adds 100 units of Star tobacco at standard rate ৳920), `"Add 250 tea stall expense"`, or `"করিম ভাই বাকিতে ৫০০ টাকা নিলো"`.
- **Google Workspace Data Import/Export**: Import product inventories from Google Docs / text catalogs, export backups to Google Drive, and sync dues with Google Calendar.

---

## 2. User Experience & Visual Design

### Aesthetic Direction & Visual Identity
- **Design Mood**: Utilitarian, trustworthy, high-contrast modern fintech aesthetic inspired by trusted accounting tools.
- **Color Palette**:
  - `Primary`: Emerald Green (`#0A6847` / `#16A34A`) representing commerce, financial clarity, and prosperity.
  - `Secondary / bKash Pink Accent`: Vivid Magenta (`#E2136E`) for bKash payment triggers and quick actions.
  - `Due / Baki Alert`: Amber Crimson (`#DC2626` / `#F59E0B`) for credit reminders and low-stock alerts.
  - `Surface & Background`: Crisp layered surfaces (`#F8FAFC` light background, `#0F172A` dark theme surface) with distinct card elevations (`tonalElevation = 2.dp`).
- **Typography & Hierarchy**:
  - Display & Key Numerics: Bold tabular monospace numerals for currency amounts (৳ / Tk), clean font hierarchy for Bengali script legibility.
  - Generous 48dp+ touch targets designed for quick counter finger-taps.

### Key User Flows & Tabs
1. **Billing & POS Counter (Home)**:
   - Floating Voice Assistant Bar at the top with quick speech triggers.
   - Quick Product Grid with search, category pills, and shortcut chips.
   - Active Bill Drawer / Bottom Sheet showing total, discounts, tax, and one-tap checkout (Cash, bKash, Baki/Credit).
   - Instant Print / Share action modal (58mm Thermal POS, A4 PDF, WhatsApp, SMS).
2. **Khata Ledger (TallyKhata)**:
   - Split view: **Customers (Got / Will Get - পাবো)** and **Suppliers (Gave / Will Give - দেবো)**.
   - Per-contact ledger timeline with balance cards, transaction history, and direct WhatsApp / SMS reminder buttons.
3. **Inventory & Stock**:
   - Product list with stock counts, purchase vs. selling prices, low-stock badges.
   - Quick stock-in / stock-out modal.
   - Bulk import parser for Google Docs / text data catalogs.
4. **Voice Assistant & Shortcuts Hub**:
   - Voice command console with live speech transcription (Bengali & English).
   - Custom Shortcut Manager (e.g. set `"star"` = "Star Cigarettes", default price `920`, default qty `100`).
5. **Reports & Workspace Sync**:
   - Daily sales summary, profit calculation, cash vs. bKash collection breakdown.
   - Google Drive backup export / restore and Google Calendar reminder sync.

---

## 3. Key Product Decisions & Trade-Offs

### 1. Hybrid Persistence: Local Room DB + Firebase Firestore Sync
- **Chosen Approach**: Store all transactions, items, contacts, and settings in a local SQLite/Room database first for zero-latency offline operation. When connected, sync changes to Firebase Firestore using user-isolated collections (`users/{userId}/...`) authenticated via Google Sign-In (Credential Manager).
- **Why**: Retail shop counters cannot afford delays or internet drops when customers are waiting in line. Offline-first guarantees 100% uptime, while Firestore provides seamless multi-device backup and restoration.

### 2. Multi-Format Invoice Generation Engine
- **Chosen Approach**: Create a unified `InvoiceRenderer` capable of generating:
  - HTML/Canvas-based receipt formatted for **58mm and 80mm ESC/POS thermal printers** via Android `PrintManager` (PrintDocumentAdapter).
  - Standard **A4 printable invoice** with merchant header, itemized table, QR code, and signatures.
  - Compact formatted text memo with bKash merchant/personal pay number and direct WhatsApp/SMS share intents.
- **Why**: Satisfies all three user-requested formats without external proprietary SDK bloat.

### 3. Bilingual Voice Parser (Local Rule Engine + Gemini AI Logic)
- **Chosen Approach**: Two-tier voice execution:
  - *Tier 1 (Instant Local Regex & Keyword Matcher)*: Recognizes mapped user shortcuts (e.g., `"star 100"`, `"add 500 cash"`, `"বাকি ৫০০"`) in <10ms without network calls.
  - *Tier 2 (Firebase AI Logic / Gemini 3.8 Flash)*: Handles conversational, fast, or nuanced natural language commands in Bengali and English (e.g., `"করিম ভাইকে বাকিতে দুই কেজি চিনি ও এক লিটার সয়াবিন তেল দাও"` or `"Add 450 taka expense for transport"`), extracting structured JSON action payloads.
- **Why**: Provides instant zero-lag responsiveness for frequent counter commands while leveraging Gemini for complex conversational instructions.

### 4. Direct bKash & Messaging Integration
- **Chosen Approach**: Android Intent-based integration for bKash (merchant USSD code `*247#` / App intent / deep-link generation), WhatsApp (`com.whatsapp` intent with pre-filled message and invoice document), and Android SMS (`Telephony` / `ACTION_SENDTO`).
- **Why**: Avoids requiring heavy merchant gateway contracts on the device while giving the shopkeeper immediate real-world utility on personal or merchant numbers.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                   HishabKhata & BillBook Architecture                  │
└────────────────────────────────────────────────────────────────────────┘
                                 │
     ┌───────────────────────────┴───────────────────────────┐
     ▼                                                       ▼
┌───────────────────────────────┐       ┌────────────────────────────────┐
│   Jetpack Compose M3 UI       │       │    Bilingual Voice Assistant   │
│  - POS Counter & Active Bill  │       │  - Android SpeechRecognizer    │
│  - Customer & Supplier Khata  │◄──────┤  - Local Regex Shortcut Matcher│
│  - Stock & Inventory Manager  │       │  - Firebase AI Logic (Gemini)  │
│  - Invoice Print & Share Hub  │       └────────────────────────────────┘
└───────────────┬───────────────┘
                │ ViewModels & StateFlows
                ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          Repository Layer                              │
│  - BillingRepository (Invoices, Items, Shortcuts)                      │
│  - KhataRepository (Customers, Suppliers, Ledger Entries)              │
│  - PrintAndShareManager (Thermal 58mm, A4 PrintManager, WhatsApp/SMS)  │
│  - GoogleWorkspaceSyncManager (Docs Importer, Drive/Calendar Exporter) │
└───────────────┬────────────────────────────────────────┬───────────────┘
                ▼                                        ▼
┌───────────────────────────────┐       ┌────────────────────────────────┐
│     Room Local Database       │       │  Firebase Cloud Infrastructure │
│  - ProductEntity & StockLogs  │◄─────►│  - Google Sign-In Auth         │
│  - CustomerEntity & Ledgers   │ (Sync)│  - Firestore Isolated Storage  │
│  - InvoiceEntity & LineItems  │       │  - App Check & AI Logic        │
│  - VoiceShortcutEntity        │       │  - Security Rules (User Scope) │
└───────────────────────────────┘       └────────────────────────────────┘
```

### Data Entities
1. **Product / Stock**: `id`, `name`, `nameBn`, `barcode`, `purchasePrice`, `sellingPrice`, `stockQty`, `unit` (kg, pc, packet, liter), `lowStockThreshold`.
2. **Invoice / Bill**: `invoiceNumber`, `customerName`, `customerPhone`, `items: List<InvoiceItem>`, `subtotal`, `discount`, `tax`, `totalAmount`, `paidAmount`, `dueAmount`, `paymentMode` (Cash, bKash, Due), `timestamp`.
3. **Khata Contact**: `id`, `name`, `phone`, `type` (CUSTOMER / SUPPLIER), `currentBalance` (Positive = Customer owes merchant; Negative = Merchant owes supplier), `lastUpdated`.
4. **Ledger Transaction**: `id`, `contactId`, `type` (GAVE / GOT), `amount`, `note`, `invoiceId?`, `timestamp`.
5. **Voice Shortcut**: `id`, `triggerWord` (e.g., `"star"`, `"sugar"`, `"চা"`), `targetProductId`, `defaultQuantity`, `defaultPrice`.

### Verification Plan
1. **Build & Compilation**: Verify with `compile_applet` ensuring all Compose, Room KSP, Firebase, and PrintManager modules compile cleanly.
2. **Offline Mode & Persistence Test**: Validate that adding products, invoices, and ledger entries works without network connectivity.
3. **Voice Recognition & Shortcut Execution**: Test shortcut mapping (e.g. `"star 100"`) and bilingual voice command processing.
4. **Print & Share Verification**: Test generation of 58mm thermal preview, A4 print intent, and WhatsApp/SMS message builders.
