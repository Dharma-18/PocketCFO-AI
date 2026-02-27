import json

def parse_business_logic(user_input):
    # This is a MOCK response for the demo
    return {
        "receipt_date": "Oct 24, 2023",
        "transactions": [
            { "item": "Flour Supply", "amount": 500, "type": "Expense", "category": "Inventory" },
            { "item": "Cakes Sold", "amount": 800, "type": "Income", "category": "Sales" }
        ],
        "net_balance": 300,
        "micro_insight": "Your flour costs are stable, but cake sales are up 10% from yesterday!"
    }