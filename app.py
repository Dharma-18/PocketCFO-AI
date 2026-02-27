import streamlit as st
from core.ai_engine import parse_business_logic

# 1. Page Config
st.set_page_config(page_title="PocketCFO AI", page_icon="📊", layout="centered")

# 2. Advanced Styling (To match your prototype)
st.markdown("""
    <style>
    .main { background-color: #f8f9fa; }
    .stTextInput > div > div > input { border-radius: 20px; }
    
    /* Chat Bubbles */
    .user-bubble {
        background-color: #1E40AF;
        color: white;
        padding: 15px;
        border-radius: 15px 15px 0px 15px;
        margin: 10px 0;
        width: 80%;
        float: right;
        font-family: 'sans-serif';
    }
    
    /* Receipt Card */
    .receipt-card {
        background-color: white;
        border-radius: 15px;
        padding: 20px;
        box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        border: 1px solid #e0e0e0;
        margin-top: 20px;
    }
    
    /* Insight Banner */
    .insight-banner {
        background-color: #FFFBEB;
        border-left: 5px solid #F59E0B;
        padding: 15px;
        border-radius: 8px;
        color: #92400E;
        font-weight: bold;
        margin-bottom: 20px;
    }
    
    .status-tag {
        background-color: #D1FAE5;
        color: #065F46;
        padding: 2px 8px;
        border-radius: 5px;
        font-size: 10px;
        font-weight: bold;
        float: right;
    }
    </style>
""", unsafe_allow_html=True)

# 3. Header
st.markdown("<h1 style='text-align: center; color: #1E3A8A;'>📱 PocketCFO AI</h1>", unsafe_allow_html=True)
st.markdown("<p style='text-align: center; color: #6B7280;'>Agentic Financial Companion</p>", unsafe_allow_html=True)

# 4. Input Area
with st.container():
    user_msg = st.text_input("Log your day...", placeholder="e.g. Spent 500 on flour and sold 10 cakes for 800")
    send_btn = st.button("Analyze Transaction", use_container_width=True)

if send_btn and user_msg:
    with st.spinner("CFO is analyzing..."):
        try:
            # AI Logic
            res = parse_business_logic(user_msg)
            
            # DISPLAY 1: User Message
            st.markdown(f'<div class="user-bubble">{user_msg}</div>', unsafe_allow_html=True)
            st.write("") # Spacer
            
            # DISPLAY 2: Insight Banner
            st.markdown(f'<div class="insight-banner">💡 MICRO-INSIGHT: {res["micro_insight"]}</div>', unsafe_allow_html=True)
            
            # DISPLAY 3: Professional Receipt Card (Matches your design)
            with st.container():
                st.markdown(f"""
                <div class="receipt-card">
                    <span class="status-tag">VERIFIED</span>
                    <p style="color: #6B7280; font-size: 12px; margin-bottom: 5px;">DAILY RECEIPT</p>
                    <h4 style="margin-top: 0;">{res['receipt_date']}</h4>
                    <hr>
                """, unsafe_allow_html=True)
                
                for tx in res['transactions']:
                    color = "#EF4444" if tx['type'] == "Expense" else "#10B981"
                    symbol = "-" if tx['type'] == "Expense" else "+"
                    st.markdown(f"""
                        <div style="display: flex; justify-content: space-between;">
                            <span><b>{tx['item']}</b> ({tx['category']})</span>
                            <span style="color: {color}; font-weight: bold;">{symbol}${tx['amount']}</span>
                        </div>
                    """, unsafe_allow_html=True)
                
                st.markdown(f"""
                    <hr>
                    <div style="display: flex; justify-content: space-between; font-size: 18px; font-weight: bold;">
                        <span>Net Balance</span>
                        <span>${res['net_balance']}</span>
                    </div>
                </div>
                """, unsafe_allow_html=True)

        except Exception as e:
            st.error(f"Error: {e}")

# 5. Dashboard Preview (Static for 30% phase)
st.write("")
st.write("---")
st.subheader("Financial Pulse")
col1, col2 = st.columns(2)
col1.metric("Weekly Profit", "$4,250", "+12.5%")
col2.metric("Cash Flow", "Healthy", "7 Days")