const express = require('express');

const router = express.Router();

const citiesRouter = require('./cities');
const airportsRouter = require('./airports');
const bookingsRouter = require('./bookings');
const flightsRouter = require('./flights');
const checkinsRouter = require('./checkins');

router.use('/cities', citiesRouter);
router.use('/airports', airportsRouter);
router.use('/bookings', bookingsRouter);
router.use('/flights', flightsRouter);
router.use('/checkins', checkinsRouter);

module.exports = router;