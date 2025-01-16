const express = require('express');
const cors = require('cors');
const Stripe = require('stripe');

// Replace with your actual Stripe Secret Key
const stripe = Stripe('sk_test_51QeVY6LgoAKhLV6iDOoyLaK7Uz1zyTGyFg9mIJTnFWYSoTJUPNOrnpzcy9qxjksXQ7feJXdKAZYalU2xIxjUu8AJ00h3Pa4OFi');

const app = express();

app.use(cors());
app.use(express.json());

// Endpoint to create a PaymentIntent
app.post('/create-payment-intent', async (req, res) => {
    try {
        const { amount } = req.body;

        // Validate amount
        if (!amount || amount <= 0) {
            return res.status(400).send({ error: 'Invalid amount' });
        }

        // Create a PaymentIntent with Stripe
        const paymentIntent = await stripe.paymentIntents.create({
            amount: amount,
            currency: 'usd',
            payment_method_types: ['card'],
        });

        // Send clientSecret to the client
        res.status(200).send({
            clientSecret: paymentIntent.client_secret,
        });
    } catch (error) {
        console.error('Error creating PaymentIntent:', error);
        res.status(500).send({
            error: error.message,
        });
    }
});

// Start the server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
