const Booking = require('../models/Booking');

const createBooking = async (req, res, next) => {
  try {
    const { flightNo, flightDate, fareConditions, passengerId, passengerName, outbound } = req.body;
    const booking = await Booking.createBooking({ flightNo, flightDate, fareConditions, passengerId, passengerName, outbound });
    res.status(201);
    res.json(booking);
  } catch (error) {
    next(error);
  }
}

module.exports = { createBooking };